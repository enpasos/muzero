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

package ai.enpasos.muzero.platform.run.train;


import ai.djl.Device;
import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.refcount.RCScope;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.djl.training.loss.SimpleCompositeLoss;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.djl.MyEasyTrain;
import ai.enpasos.muzero.platform.agent.intuitive.djl.MyEpochTrainingListener;
import ai.enpasos.muzero.platform.agent.intuitive.djl.MyIndexLoss;
import ai.enpasos.muzero.platform.agent.intuitive.djl.MySoftmaxCrossEntropyLoss;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import ai.enpasos.muzero.platform.config.TrainingTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper.getEpochFromModel;
import static ai.enpasos.muzero.platform.common.Constants.TRAIN_ALL;

@Slf4j
@Component
public class MuZero {

    @Autowired
    MuZeroConfig config;

    @Autowired
    SelfPlay selfPlay;

    @Autowired
    ReplayBuffer replayBuffer;


    @Autowired
    NetworkHelper networkHelper;

//    private static void setTrainingTypeKeyOnTrainer(Trainer trainer ) {
//        SimpleCompositeLoss loss = (SimpleCompositeLoss) trainer.getLoss();
//        List<MySoftmaxCrossEntropyLoss> losses = loss.getComponents().stream()
//            .map(c -> ((MyIndexLoss) c).getLoss())
//            .filter(c -> c.getName().contains("loss_policy")).map(c -> (MySoftmaxCrossEntropyLoss) c).toList();
//        losses.stream().forEach(l -> l.setUseLabelAsLegalCategoriesFilter(false));
//    }

    public void play(Network network, boolean render, boolean justInitialInferencePolicy, boolean withRandomActions) {

        selfPlay.playMultipleEpisodes(network, render, false, justInitialInferencePolicy, withRandomActions);
    }

    public void initialFillingBuffer(Network network) {

        long startCounter = replayBuffer.getBuffer().getCounter();
        int windowSize = config.getWindowSize();
        while (replayBuffer.getBuffer().getCounter() - startCounter < windowSize) {
            log.info(replayBuffer.getBuffer().getGames().size() + " of " + windowSize);
            selfPlay.playMultipleEpisodes(network, false, true, false, false);
        }
    }

    public void createNetworkModelIfNotExisting() {
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            if (model.getBlock() == null) {
                MuZeroBlock block = new MuZeroBlock(config);
                model.setBlock(block);
                loadOrCreateModelParameters(model);
            }
        } catch (Exception e) {
            String message = "not able to save created model";
            log.error(message);
            throw new MuZeroException(message, e);
        }
    }



    private void loadOrCreateModelParameters( Model model) {
        try {
            String outputDir = config.getNetworkBaseDir();
            model.load(Paths.get(outputDir));
            replayBuffer.createNetworkNameFromModel(model, model.getName(), outputDir);
        } catch (Exception e) {
            try (RCScope rcScope1 = new RCScope()) {
                trainNetwork(model);
            }
            String outputDir = config.getNetworkBaseDir();
            try {
                model.load(Paths.get(outputDir));
            } catch (Exception ex) {
                throw new MuZeroException(ex);
            }
            replayBuffer.createNetworkNameFromModel(model, model.getName(), outputDir);
        }
    }

    public void deleteNetworksAndGames() {
        try {
            FileUtils.forceDelete(new File(config.getOutputDir()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileUtils.forceMkdir(new File(config.getNetworkBaseDir()));
            FileUtils.forceMkdir(new File(config.getGamesBasedir()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("java:S106")
    public void train(TrainParams params) {

        int trainingStep = 0;
        int epoch = 0;

        List<DurAndMem> durations = new ArrayList<>();

        loadBuffer(params.freshBuffer);

        try (RCScope rcScope0 = new RCScope()) {
            try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
                Network network = new Network(config, model);
                if (!params.withoutFill) {
                    initialFillingBuffer(network);
                }
            }
        }
        try (RCScope rcScope0 = new RCScope()) {
            try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
                createNetworkModelIfNotExisting();
            }
        }

        try (RCScope rcScope0 = new RCScope()) {
            try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {

                Network network = new Network(config, model);
                createNetworkModelIfNotExisting();

                epoch = NetworkHelper.getEpochFromModel(model);
                int epochStart = epoch;

                while (trainingStep < config.getNumberOfTrainingSteps() &&
                    (config.getNumberOfEpisodesPerJVMStart() <= 0 || epoch - epochStart < config.getNumberOfEpisodesPerJVMStart())) {

                    DurAndMem duration = new DurAndMem();
                    duration.on();
                    int i = 0;

                    if (!params.freshBuffer) {

                        try (RCScope rcScope1 = new RCScope()) {
                            for (PlayTypeKey key : config.getPlayTypeKeysForTraining()) {
                                config.setPlayTypeKey(key);
                                playGames(params.render, network, trainingStep);
                            }
                        }

                        int n = replayBuffer.getBuffer().getGames().size();
                        int m = (int) replayBuffer.getBuffer().getGames().stream().filter(g ->
                            g.getGameDTO().getTStateB() != 0
                        ).count();
                        log.info("games with an alternative action " + m + " out of " + n);
                        log.info("counter: " + replayBuffer.getBuffer().getCounter());
                        log.info("window size: " + replayBuffer.getBuffer().getWindowSize());


                        log.info("replayBuffer size: " + this.replayBuffer.getBuffer().getGames().size());
                    }
                    params.getAfterSelfPlayHookIn().accept(networkHelper.getEpoch(), network);
                    try (RCScope rcScope1 = new RCScope()) {
                        trainingStep = trainNetwork(model);
                    }


                    if (i % 5 == 0) {
                        params.getAfterTrainingHookIn().accept(networkHelper.getEpoch(), model);
                    }
                    i++;
                    duration.off();
                    durations.add(duration);
                    System.out.println("epoch;duration[ms];gpuMem[MiB]");
                    IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));


                    epoch = NetworkHelper.getEpochFromModel(model);
                }
            }
        }
    }

    public void loadBuffer(boolean freshBuffer) {
        replayBuffer.init();
        if (!freshBuffer) {
            replayBuffer.loadLatestState();
        }
    }





    int trainNetwork(Model model) {
        int epoch;
        int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
        boolean withSymmetryEnrichment = true;
            epoch = getEpochFromModel(model);
            DefaultTrainingConfig djlConfig = networkHelper.setupTrainingConfig(epoch);
            int finalEpoch = epoch;
            djlConfig.getTrainingListeners().stream()
                .filter(MyEpochTrainingListener.class::isInstance)
                .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(finalEpoch));
            try (Trainer trainer = model.newTrainer(djlConfig)) {
                Shape[] inputShapes = networkHelper.getInputShapes();
                trainer.initialize(inputShapes);
                trainer.setMetrics(new Metrics());

                for (int m = 0; m < numberOfTrainingStepsPerEpoch; m++) {
                    try (Batch batch = networkHelper.getBatch(trainer.getManager(), withSymmetryEnrichment)) {
                        log.debug("trainBatch " + m);
                        MyEasyTrain.trainBatch(trainer, batch);
                        trainer.step();
                    }
                }

                // number of action paths
                int numActionPaths = this.replayBuffer.getBuffer().getNumOfDifferentGames();
                model.setProperty("NumActionPaths", Double.toString(numActionPaths));
                log.info("NumActionPaths: " + numActionPaths);

                handleMetrics(trainer, model, epoch);

                trainer.notifyListeners(listener -> listener.onEpoch(trainer));
            }
       // }
        epoch = getEpochFromModel(model);
        return epoch * numberOfTrainingStepsPerEpoch;

    }

    private void handleMetrics(Trainer trainer, Model model, int epoch) {
        Metrics metrics = trainer.getMetrics();

        // mean value
        // loss
        double meanValueLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("value_0"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        replayBuffer.putMeanValueLoss(epoch, meanValueLoss);

        // mean similarity
        // loss
        double meanSimLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("loss_similarity_0"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        replayBuffer.putMeanValueLoss(epoch, meanSimLoss);

        log.info("MeanSimilarityLoss: " + meanSimLoss);

        // mean policy loss
        double meanPolicyLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("policy_0"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();

        log.info("MeanPolicyLoss: " + meanPolicyLoss);
    }

    void playGames(boolean render, Network network, int trainingStep) {
        if (trainingStep != 0 && trainingStep > config.getNumberTrainingStepsOnStart()) {
            log.info("last training step = {}", trainingStep);
            log.info("numSimulations: " + config.getNumSimulations());
            network.debugDump();
            boolean justInitialInferencePolicy = config.getNumSimulations() == 0;

            play(network, render, justInitialInferencePolicy, true);

        }
    }


}
