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
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.djl.MySaveModelTrainingListener;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.AltStarts;
import ai.enpasos.muzero.platform.run.Surprise;
import ai.enpasos.muzero.platform.run.ValueSelfconsistency;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

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
    Surprise surprise;

    @Autowired
    AltStarts altStarts;


    @Autowired
    ValueSelfconsistency valueSelfconsistency;


    @Autowired
    NetworkHelper networkHelper;

    public void play(Network network, boolean render, boolean justInitialInferencePolicy, boolean withRandomActions) {

        selfPlay.playMultipleEpisodes(network, render, false, justInitialInferencePolicy, withRandomActions);
    }

    public void initialFillingBuffer(Network network) {

        int windowSize = config.getWindowSize();
        long startCounter = replayBuffer.getBuffer().getCounter();
        while (replayBuffer.getBuffer().getCounter() - startCounter < windowSize) {
            log.info(replayBuffer.getBuffer().getGames().size() + " of " + windowSize);
            selfPlay.playMultipleEpisodes(network, false, true, false, false);
            replayBuffer.saveState();
        }
    }

    public void createNetworkModelIfNotExisting() {
        int epoch = 0;
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            if (model.getBlock() == null) {
                MuZeroBlock block = new MuZeroBlock(config);
                model.setBlock(block);
                loadOrCreateModelParameters(epoch, model);
            }
        } catch (Exception e) {
            String message = "not able to save created model";
            log.error(message);
            throw new MuZeroException(message, e);
        }
    }

    private void loadOrCreateModelParameters(int epoch, Model model) {
        try {
            String outputDir = config.getNetworkBaseDir();
            model.load(Paths.get(outputDir));
            replayBuffer.createNetworkNameFromModel(model, model.getName(), outputDir);
        } catch (Exception e) {
            createNetworkModel(epoch, model);
        }
    }

    private void createNetworkModel(int epoch, Model model) {
        log.info("*** no existing model has been found ***");
        DefaultTrainingConfig djlConfig = networkHelper.setupTrainingConfig(epoch);
        try (Trainer trainer = model.newTrainer(djlConfig)) {
            Shape[] inputShapes = networkHelper.getInputShapes();
            trainer.initialize(inputShapes);
            model.setProperty("Epoch", String.valueOf(epoch));
            log.info("*** new model is stored in file      ***");
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

    public void train(TrainParams params) {
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            Network network = new Network(config, model);

            init(params.freshBuffer, params.randomFill, network, params.withoutFill);

            int epoch = networkHelper.getEpoch();
            int trainingStep = config.getNumberOfTrainingStepsPerEpoch() * epoch;
            DefaultTrainingConfig djlConfig = networkHelper.setupTrainingConfig(epoch);

            int i = 0;
            while (trainingStep < config.getNumberOfTrainingSteps()) {
                if (!params.freshBuffer) {


                   // altStarts.reset();

                    playGames(params.render, network, trainingStep);

                    altStarts.playGames(network, trainingStep);

                    surprise.getSurpriseThresholdAndShowSurpriseStatistics(this.replayBuffer.getBuffer().getGames());

                    //  if (trainingStep > 1000) {
//                        surpriseCheck(network);
                    //   }

//                    if (config.isSurpriseHandlingOn() && networkHelper.getEpoch() > 1) {
//                        surprise.handleOldSurprises(network);
//                        surprise.markSurprise();
//                    }

                    log.info("replayBuffer size: " + this.replayBuffer.getBuffer().getGames().size());

                    if (config.isExtraValueTrainingOn()) {
                        double temp = config.getTemperatureRoot();
                        int sims = config.getNumSimulations();
                        config.setNumSimulations(0);
                        config.setTemperatureRoot(1);
                        playGames(params.render, network, trainingStep);
                        config.setNumSimulations(sims);
                        config.setTemperatureRoot(temp);
                    }

                }
                params.getAfterSelfPlayHookIn().accept(networkHelper.getEpoch(), network);
                trainingStep = trainNetwork(params.numberOfEpochs, model, djlConfig);
                if (config.isSurpriseHandlingOn()) {
                    surpriseCheck(network);
                }

                if (i % 5 == 0) {
                    params.getAfterTrainingHookIn().accept(networkHelper.getEpoch(), model);
                }
                i++;
            }
        }
    }

    private void surpriseCheck(Network network) {
        List<Game> gamesForThreshold = this.replayBuffer.getBuffer().getGames().stream()
            .filter(game -> game.getGameDTO().getSurprises().size() > 0)
            .filter(game -> game.getGameDTO().getTStateA() == 0).collect(Collectors.toList());
        double surpriseThreshold = surprise.getSurpriseThresholdAndShowSurpriseStatistics(gamesForThreshold);
        log.info("surpriseThreshold: " + surpriseThreshold);
        long c = this.replayBuffer.getBuffer().getCounter();
        List<Game> gamesToCheck = this.replayBuffer.getBuffer().getGames().stream()
            .filter(game -> {
                    long nextCheck = game.getGameDTO().getNextSurpriseCheck();
                    if (nextCheck == 0) {
                        game.getGameDTO().setNextSurpriseCheck(this.config.getSurpriseCheckInterval());
                        nextCheck = game.getGameDTO().getNextSurpriseCheck();
                    }
                    return nextCheck < c;
                }
            ).collect(Collectors.toList());

        log.info("start surprise.measureValueAndSurprise of " + gamesToCheck.size() + " games");
        surprise.measureValueAndSurprise(network, gamesToCheck);
        log.info("end surprise.measureValueAndSurprise");

        gamesToCheck.stream().forEach(game -> {
            game.getGameDTO().setNextSurpriseCheck(game.getGameDTO().getNextSurpriseCheck() + config.getSurpriseCheckInterval());
        });


        identifyGamesWithSurprise(this.replayBuffer.getBuffer().getGames(), surpriseThreshold, 0);

    }

    @NotNull
    public void init(boolean freshBuffer, boolean randomFill, Network network, boolean withoutFill) {
        createNetworkModelIfNotExisting();
        replayBuffer.init();
        if (freshBuffer) {
            while (!replayBuffer.getBuffer().isBufferFilled()) {
                network.debugDump();
                play(network, false, true, false);
                replayBuffer.saveState();
            }
        } else {
            replayBuffer.loadLatestState();
            if (withoutFill) return;
            if (randomFill) {
                initialFillingBuffer(network);
            } else {
                play(network, false, false, false);
                replayBuffer.saveState();
            }
        }
    }

    @NotNull
    private void init2(Network network) {
        createNetworkModelIfNotExisting();

        replayBuffer.init();

    }

    int trainNetwork(int numberOfEpochs, Model model, DefaultTrainingConfig djlConfig) {
        int trainingStep;
        int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
        int epoch = getEpochFromModel(model);
        boolean withSymmetryEnrichment = true;


        int finalEpoch = epoch;
        djlConfig.getTrainingListeners().stream()
            .filter(MySaveModelTrainingListener.class::isInstance)
            .forEach(trainingListener -> ((MySaveModelTrainingListener) trainingListener).setEpoch(finalEpoch));

        try (Trainer trainer = model.newTrainer(djlConfig)) {

            trainer.setMetrics(new Metrics());
            Shape[] inputShapes = networkHelper.getInputShapes();
            trainer.initialize(inputShapes);

            for (int i = 0; i < numberOfEpochs; i++) {
                for (int m = 0; m < numberOfTrainingStepsPerEpoch; m++) {
                    try (Batch batch = networkHelper.getBatch(trainer.getManager(), withSymmetryEnrichment)) {
                        log.debug("trainBatch " + m);
                        EasyTrain.trainBatch(trainer, batch);
                        trainer.step();
                    }
                }
                Metrics metrics = trainer.getMetrics();

                // mean loss
                List<Metric> ms = metrics.getMetric("train_all_CompositeLoss");
                double meanLoss = ms.stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new);
                model.setProperty("MeanLoss", Double.toString(meanLoss));
                log.info("MeanLoss: " + meanLoss);

                // mean value
                // loss
                double meanValueLoss = metrics.getMetricNames().stream()
                    .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("value_0"))
                    .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                    .sum();
                meanValueLoss += metrics.getMetricNames().stream()
                    .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("value_0") && name.contains("value"))
                    .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                    .sum();
                model.setProperty("MeanValueLoss", Double.toString(meanValueLoss));
                log.info("MeanValueLoss: " + meanValueLoss);

                // mean policy loss
                double meanPolicyLoss = metrics.getMetricNames().stream()
                    .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("policy_0"))
                    .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                    .sum();
                meanPolicyLoss += metrics.getMetricNames().stream()
                    .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("policy_0") && name.contains("policy"))
                    .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                    .sum();
                model.setProperty("MeanPolicyLoss", Double.toString(meanPolicyLoss));
                log.info("MeanPolicyLoss: " + meanPolicyLoss);

                trainer.notifyListeners(listener -> listener.onEpoch(trainer));

            }


        }
        epoch = getEpochFromModel(model);
        trainingStep = epoch * numberOfTrainingStepsPerEpoch;
        return trainingStep;
    }

    void playGames(boolean render, Network network, int trainingStep) {
        if (trainingStep != 0 && trainingStep > config.getNumberTrainingStepsOnStart()) {
            log.info("last training step = {}", trainingStep);
            log.info("numSimulations: " + config.getNumSimulations());
            network.debugDump();
            boolean justInitialInferencePolicy = config.getNumSimulations() == 0;
            play(network, render, justInitialInferencePolicy, true);
            replayBuffer.saveState();
        }
    }

    @NotNull
    public List<Game> identifyGamesWithSurprise(List<Game> games, double surpriseThreshold, int offset) {
        List<Game> gamesWithSurprise = surprise.getGamesWithSurprisesAboveThreshold(games, surpriseThreshold);


        games.stream().forEach(game -> {
            game.getGameDTO().setSurprised(false);
            game.getGameDTO().setTStateA(0);
            game.getGameDTO().setTStateB(0);
        });
        gamesWithSurprise.stream().forEach(game -> {
            game.getGameDTO().setSurprised(true);
            long t = game.getGameDTO().getTSurprise();
            long t0 = Math.max(t + offset, 0);
            game.getGameDTO().setTStateA(t0);
            game.getGameDTO().setTStateB(t0);
        });
        return gamesWithSurprise;
    }

}
