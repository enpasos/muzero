/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ai.enpasos.muzero.platform;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.djl.MySaveModelTrainingListener;
import ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.slow.play.SelfPlay;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper.*;
import static ai.enpasos.muzero.platform.common.Constants.TRAIN_ALL;

@Slf4j
public class MuZero {


    private MuZero() {
    }

    public static void playOnDeepThinking(Network network, ReplayBuffer replayBuffer, boolean render) {
        MuZeroConfig config = network.getConfig();
        SelfPlay.playMultipleEpisodes(network, replayBuffer, config, render, false, true);
    }

    public static void initialFillingBuffer(Network network, ReplayBuffer replayBuffer) {
        MuZeroConfig config = network.getConfig();

        while (replayBuffer.getBuffer().getData().size() < config.getWindowSize()) {
            log.info(replayBuffer.getBuffer().getData().size() + " of " + config.getWindowSize());
            SelfPlay.playMultipleEpisodes(network, replayBuffer, config, false, true, true);
            replayBuffer.saveState();
        }
    }

    public static void createNetworkModelIfNotExisting(@NotNull MuZeroConfig config) {
        int epoch = 0;
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            if (model.getBlock() == null) {
                MuZeroBlock block = new MuZeroBlock(config);
                model.setBlock(block);
                loadOrCreateModelParameters(config, epoch, model);
            }
        } catch (Exception e) {
            String message = "not able to save created model";
            log.error(message);
            throw new MuZeroException(message, e);
        }
    }

    private static void loadOrCreateModelParameters(@NotNull MuZeroConfig config, int epoch, Model model) {
        try {
            model.load(Paths.get(getNetworksBasedir(config)));
        } catch (Exception e) {
            createNetworkModel(config, epoch, model);
        }
    }

    private static void createNetworkModel(@NotNull MuZeroConfig config, int epoch, Model model) {
        log.info("*** no existing model has been found ***");
        DefaultTrainingConfig djlConfig = setupTrainingConfig(config, epoch);
        try (Trainer trainer = model.newTrainer(djlConfig)) {
            Shape[] inputShapes = getInputShapes(config);
            trainer.initialize(inputShapes);
            model.setProperty("Epoch", String.valueOf(epoch));
            log.info("*** new model is stored in file      ***");
        }
    }


    public static void deleteNetworksAndGames(@NotNull MuZeroConfig config) {
        try {
            FileUtils.forceDelete(new File(config.getOutputDir()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileUtils.forceMkdir(new File(getNetworksBasedir(config)));
            FileUtils.forceMkdir(new File(getGamesBasedir(config)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static @NotNull String getGamesBasedir(@NotNull MuZeroConfig config) {
        return config.getOutputDir() + "games";
    }


    public static @NotNull String getNetworksBasedir(@NotNull MuZeroConfig config) {
        if (config.getNetworkBaseDir() != null) return config.getNetworkBaseDir();
        return config.getOutputDir() + "networks";
    }

    public static void train(MuZeroConfig config, boolean freshBuffer, int numberOfEpochs) {
        train(config, freshBuffer, numberOfEpochs, false);
    }

    public static void train(MuZeroConfig config, boolean freshBuffer, int numberOfEpochs, boolean render) {
        train(config, freshBuffer, numberOfEpochs, render, true);
    }

    public static void train(MuZeroConfig config, boolean freshBuffer, int numberOfEpochs, boolean render, boolean randomFill) {
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            Network network = new Network(config, model);

            ReplayBuffer replayBuffer = init(config, freshBuffer, randomFill, network);

            int trainingStep = NetworkHelper.numberOfLastTrainingStep(config);
            DefaultTrainingConfig djlConfig = setupTrainingConfig(config, 0);

            while (trainingStep < config.getNumberOfTrainingSteps()) {
                playGames(config, render, network, replayBuffer, trainingStep);
                trainingStep = trainNetwork(config, numberOfEpochs, model, replayBuffer, djlConfig);
            }
        }
    }

    @NotNull
    private static ReplayBuffer init(MuZeroConfig config, boolean freshBuffer, boolean randomFill, Network network) {
        MuZero.createNetworkModelIfNotExisting(config);

        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        if (freshBuffer) {
            while (!replayBuffer.getBuffer().isBufferFilled()) {
                network.debugDump();
                MuZero.playOnDeepThinking(network, replayBuffer, false);
                replayBuffer.saveState();
            }
        } else {
            replayBuffer.loadLatestState();
            if (randomFill) {
                MuZero.initialFillingBuffer(network, replayBuffer);
            } else {
                MuZero.playOnDeepThinking(network, replayBuffer, false);
                replayBuffer.saveState();
            }
        }
        return replayBuffer;
    }

    private static int trainNetwork(MuZeroConfig config, int numberOfEpochs, Model model, ReplayBuffer replayBuffer, DefaultTrainingConfig djlConfig) {
        int trainingStep;
        int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
        int epoch = 0;
        boolean withSymmetryEnrichment = true;

        String prop = model.getProperty("Epoch");
        if (prop != null) {
            epoch = Integer.parseInt(prop);
        }

        int finalEpoch = epoch;
        djlConfig.getTrainingListeners().stream()
                .filter(MySaveModelTrainingListener.class::isInstance)
                .forEach(trainingListener -> ((MySaveModelTrainingListener) trainingListener).setEpoch(finalEpoch));

        try (Trainer trainer = model.newTrainer(djlConfig)) {

            trainer.setMetrics(new Metrics());
            Shape[] inputShapes = getInputShapes(config);
            trainer.initialize(inputShapes);

            for (int i = 0; i < numberOfEpochs; i++) {
                for (int m = 0; m < numberOfTrainingStepsPerEpoch; m++) {
                    try (Batch batch = getBatch(config, trainer.getManager(), replayBuffer, withSymmetryEnrichment)) {
                        log.debug("trainBatch " + m);
                        EasyTrain.trainBatch(trainer, batch);
                        trainer.step();
                    }

                }
                Metrics metrics = trainer.getMetrics();

                // mean loss
                List<Metric> ms = metrics.getMetric("train_all_CompositeLoss");
                double meanLoss = ms.stream().mapToDouble(m -> m.getValue().doubleValue()).average().orElseThrow(MuZeroException::new);
                model.setProperty("MeanLoss", Double.toString(meanLoss));
                log.info("MeanLoss: " + meanLoss);

                // mean value
                // loss
                double meanValueLoss = metrics.getMetricNames().stream()
                        .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("value_0"))
                        .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().orElseThrow(MuZeroException::new))
                        .sum();
                meanValueLoss += metrics.getMetricNames().stream()
                        .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("value_0") && name.contains("value"))
                        .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().orElseThrow(MuZeroException::new))
                        .sum();
                model.setProperty("MeanValueLoss", Double.toString(meanValueLoss));
                log.info("MeanValueLoss: " + meanValueLoss);

                // mean policy loss
                double meanPolicyLoss = metrics.getMetricNames().stream()
                        .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("policy_0"))
                        .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().orElseThrow(MuZeroException::new))
                        .sum();
                meanPolicyLoss += metrics.getMetricNames().stream()
                        .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("policy_0") && name.contains("policy"))
                        .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().orElseThrow(MuZeroException::new))
                        .sum();
                model.setProperty("MeanPolicyLoss", Double.toString(meanPolicyLoss));
                log.info("MeanPolicyLoss: " + meanPolicyLoss);

                trainer.notifyListeners(listener -> listener.onEpoch(trainer));

            }


        }

        trainingStep = epoch * numberOfTrainingStepsPerEpoch;
        return trainingStep;
    }

    private static void playGames(MuZeroConfig config, boolean render, Network network, ReplayBuffer replayBuffer, int trainingStep) {
        if (trainingStep != 0 && trainingStep > config.getNumberTrainingStepsOnRandomPlay()) {
            log.info("last training step = {}", trainingStep);
            log.info("numSimulations: " + config.getNumSimulations());
            network.debugDump();
            MuZero.playOnDeepThinking(network, replayBuffer, render);
            replayBuffer.saveState();
        }
    }

}
