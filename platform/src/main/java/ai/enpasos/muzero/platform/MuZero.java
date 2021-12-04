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
import ai.enpasos.muzero.platform.agent.gamebuffer.GameIO;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.slow.play.PlayManager;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper.*;

@Slf4j
public class MuZero {


    public static void playOnDeepThinking(Network network, ReplayBuffer replayBuffer, boolean render) {
        MuZeroConfig config = network.getConfig();

        PlayManager.playParallel(network, replayBuffer, config, render, false, true);
    }

    public static void initialFillingBuffer(Network network, ReplayBuffer replayBuffer) {
        MuZeroConfig config = network.getConfig();


        while (replayBuffer.getBuffer().getData().size() < config.getWindowSize()) {
            log.info(replayBuffer.getBuffer().getData().size() + " of " + config.getWindowSize());
            PlayManager.playParallel(network, replayBuffer, config, false, true, true);
            replayBuffer.saveState();
        }
    }

    public static void createNetworkModelIfNotExisting(@NotNull MuZeroConfig config) {
        NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, null, 0);
    }


    private static void makeObjectDir(@NotNull MuZeroConfig config) {
        try {
            FileUtils.forceMkdir(new File(getNetworksBasedir(config) + "/" + (GameIO.getLatestObjectNo(config) + 1)));
        } catch (IOException e) {
            throw new RuntimeException(e);
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


    private static void restartApplication() throws URISyntaxException, IOException {

        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final File currentJar = new File(MuZero.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        if (!currentJar.getName().endsWith(".jar"))
            return;

        final ArrayList<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentJar.getPath());
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.start();
        System.exit(0);
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

            int trainingStep = NetworkHelper.numberOfLastTrainingStep(config);
            DefaultTrainingConfig djlConfig = setupTrainingConfig(config, 0);

            while (trainingStep < config.getNumberOfTrainingSteps()) {
                if (trainingStep != 0 && trainingStep > config.getNumberTrainingStepsOnRandomPlay()) {
                    log.info("last training step = {}", trainingStep);
                    log.info("numSimulations: " + config.getNumSimulations());
                    network.debugDump();
                    MuZero.playOnDeepThinking(network, replayBuffer, render);
                    replayBuffer.saveState();
                }
                network.debugDump();
                int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
                int epoch = 0;
                boolean withSymmetryEnrichment = true;

                String prop = model.getProperty("Epoch");
                if (prop != null) {
                    epoch = Integer.parseInt(prop);
                }

                int finalEpoch = epoch;
                djlConfig.getTrainingListeners().stream()
                        .filter(trainingListener -> trainingListener instanceof MySaveModelTrainingListener)
                        .forEach(trainingListener -> ((MySaveModelTrainingListener) trainingListener).setEpoch(finalEpoch));

                try (Trainer trainer = model.newTrainer(djlConfig)) {
                    network.debugDump();
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
                            network.debugDump();
                        }
                        Metrics metrics = trainer.getMetrics();

                        // mean loss
                        List<Metric> ms = metrics.getMetric("train_all_CompositeLoss");
                        double meanLoss = ms.stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble();
                        model.setProperty("MeanLoss", Double.toString(meanLoss));
                        log.info("MeanLoss: " + meanLoss);

                        // mean value loss
                        double meanValueLoss = metrics.getMetricNames().stream()
                                .filter(name -> name.startsWith("train_all") && name.contains("value_0"))
                                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble())
                                .sum();
                        meanValueLoss += metrics.getMetricNames().stream()
                                .filter(name -> name.startsWith("train_all") && !name.contains("value_0") && name.contains("value"))
                                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble())
                                .sum();
                        model.setProperty("MeanValueLoss", Double.toString(meanValueLoss));
                        log.info("MeanValueLoss: " + meanValueLoss);

                        // mean policy loss
                        double meanPolicyLoss = metrics.getMetricNames().stream()
                                .filter(name -> name.startsWith("train_all") && name.contains("policy_0"))
                                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble())
                                .sum();
                        meanPolicyLoss += metrics.getMetricNames().stream()
                                .filter(name -> name.startsWith("train_all") && !name.contains("policy_0") && name.contains("policy"))
                                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble())
                                .sum();
                        model.setProperty("MeanPolicyLoss", Double.toString(meanPolicyLoss));
                        log.info("MeanPolicyLoss: " + meanPolicyLoss);

                        trainer.notifyListeners(listener -> listener.onEpoch(trainer));

                    }

                    network.debugDump();
                }
                network.debugDump();
                trainingStep = epoch * numberOfTrainingStepsPerEpoch;

            }

        }
    }

}
