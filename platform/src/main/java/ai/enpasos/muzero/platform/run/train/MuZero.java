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
import ai.djl.ndarray.NDScope;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.b_planning.PlayParameters;
import ai.enpasos.muzero.platform.agent.b_planning.service.PlayService;
import ai.enpasos.muzero.platform.agent.c_model.ModelState;
import ai.enpasos.muzero.platform.agent.c_model.Network;
import ai.enpasos.muzero.platform.agent.c_model.djl.MyEasyTrain;
import ai.enpasos.muzero.platform.agent.c_model.djl.MyEpochTrainingListener;
import ai.enpasos.muzero.platform.agent.c_model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.c_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.c_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.d_experience.Game;
import ai.enpasos.muzero.platform.agent.d_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.b_planning.SelfPlay;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ai.enpasos.muzero.platform.common.FileUtils2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.c_model.djl.NetworkHelper.getEpochFromModel;
import static ai.enpasos.muzero.platform.common.Constants.TRAIN_ALL;

@Slf4j
@Component
public class MuZero {

    @Autowired
    MuZeroConfig config;


    @Autowired
    SelfPlay selfPlay;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    ModelService modelService;


    @Autowired
    ModelState modelState;

    @Autowired
    NetworkHelper networkHelper;

    @Autowired
    PlayService multiGameStarter;

    public void initialFillingBuffer() {

        long startCounter = gameBuffer.getBuffer().getCounter();
        int windowSize = config.getWindowSize();
        while (gameBuffer.getBuffer().getCounter() - startCounter < windowSize) {
            log.info(gameBuffer.getBuffer().getGames().size() + " of " + windowSize);
            playMultipleEpisodes(false, true, false);


        }
    }
    public void initialFillingBuffer(Network network) {

        long startCounter = gameBuffer.getBuffer().getCounter();
        int windowSize = config.getWindowSize();
        while (gameBuffer.getBuffer().getCounter() - startCounter < windowSize) {
            log.info(gameBuffer.getBuffer().getGames().size() + " of " + windowSize);
            selfPlay.playMultipleEpisodes( false, true, false);
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
            gameBuffer.createNetworkNameFromModel(model, model.getName(), outputDir);
        } catch (Exception e) {
            try (NDScope nDScope1 = new NDScope()) {
             //   trainNetwork();
            }
            String outputDir = config.getNetworkBaseDir();
            try {
                model.load(Paths.get(outputDir));
            } catch (Exception ex) {
                throw new MuZeroException(ex);
            }
            gameBuffer.createNetworkNameFromModel(model, model.getName(), outputDir);
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


    public void loadBuffer(boolean freshBuffer) {
        gameBuffer.init();
        if (!freshBuffer) {
            gameBuffer.loadLatestState();
        }
    }


    int trainNetworkOld(Model model) {
        if (config.offPolicyCorrectionOn()) {
            determinePRatioMaxForCurrentEpoch(model);
        }

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
            int numActionPaths = this.gameBuffer.getBuffer().getNumOfDifferentGames();
            model.setProperty("NumActionPaths", Double.toString(numActionPaths));
            log.info("NumActionPaths: " + numActionPaths);

            handleMetrics(trainer, model, epoch);

            trainer.notifyListeners(listener -> listener.onEpoch(trainer));
        }
        // }
        epoch = getEpochFromModel(model);
        return epoch * numberOfTrainingStepsPerEpoch;

    }

    private void determinePRatioMaxForCurrentEpoch(Model model) {
        int epoch = getEpochFromModel(model);
        List<Game> games = this.gameBuffer.getGames().stream()
            .filter(game -> game.getGameDTO().getTrainingEpoch() == epoch && game.getPlayTypeKey() == PlayTypeKey.REANALYSE)
            .collect(Collectors.toList());
        double pRatioMax = determinePRatioMax(games);
        log.info("pRatioMaxREANALYSE({}): {}", epoch, pRatioMax);

        List<Game> games2 = this.gameBuffer.getGames().stream()
            .filter(game -> game.getGameDTO().getTrainingEpoch() == epoch && game.getPlayTypeKey() != PlayTypeKey.REANALYSE)
            .collect(Collectors.toList());
        double pRatioMax2 = determinePRatioMax(games2);
        log.info("pRatioMax({}): {}", epoch, pRatioMax);
    }

    private double determinePRatioMax(List<Game> games) {
        double pRatioMax = games.stream().mapToDouble(Game::getPRatioMax).max().orElse(1.0);
        games.stream().forEach(game -> game.setPRatioMax(pRatioMax));
        return pRatioMax;
    }



    private void handleMetrics(Trainer trainer, Model model, int epoch) {
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
        gameBuffer.putMeanValueLoss(epoch, meanValueLoss);
        meanValueLoss += metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("value_0") && name.contains("value"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        model.setProperty( "MeanValueLoss", Double.toString(meanValueLoss));
        log.info("MeanValueLoss: " + meanValueLoss);


        // mean similarity
        // loss
        double meanSimLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("loss_similarity_0"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        gameBuffer.putMeanValueLoss(epoch, meanSimLoss);
        meanSimLoss += metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("loss_similarity_0") && name.contains("loss_similarity"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        model.setProperty( "MeanSimilarityLoss", Double.toString(meanSimLoss));
        log.info("MeanSimilarityLoss: " + meanSimLoss);

        // mean policy loss
        double meanPolicyLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("policy_0"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        meanPolicyLoss += metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("policy_0") && name.contains("policy"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        model.setProperty(  "MeanPolicyLoss", Double.toString(meanPolicyLoss));
        log.info("MeanPolicyLoss: " + meanPolicyLoss);
    }

    void playGamesOld(boolean render,   int trainingStep) {
        //if (trainingStep != 0 && trainingStep > config.getNumberTrainingStepsOnStart()) {
            log.info("last training step = {}", trainingStep);
            log.info("numSimulations: " + config.getNumSimulations());

            boolean justInitialInferencePolicy = config.getNumSimulations() == 0;


            selfPlay.playMultipleEpisodes(  render, false, justInitialInferencePolicy);

       // }
    }
    void playGames(boolean render, int trainingStep) {
        //if (trainingStep != 0 && trainingStep > config.getNumberTrainingStepsOnStart()) {
        log.info("last training step = {}", trainingStep);
        log.info("numSimulations: " + config.getNumSimulations());
        // network.debugDump();
        boolean justInitialInferencePolicy = config.getNumSimulations() == 0;


        playMultipleEpisodes(render, false, justInitialInferencePolicy);

        // }
    }
    @SuppressWarnings("java:S106")
    public void trainNew(TrainParams params) throws InterruptedException, ExecutionException {

        int trainingStep = 0;
        int epoch = 0;

        List<DurAndMem> durations = new ArrayList<>();

        loadBuffer(params.freshBuffer);
        initialFillingBuffer();

        modelService.loadLatestModelOrCreateIfNotExisting().get();
        epoch = modelState.getEpoch();
        int epochStart = epoch;

        while (trainingStep < config.getNumberOfTrainingSteps() &&
                (config.getNumberOfEpisodesPerJVMStart() <= 0 || epoch - epochStart < config.getNumberOfEpisodesPerJVMStart())) {

            DurAndMem duration = new DurAndMem();
            duration.on();

            if (!params.freshBuffer) {
                if (epoch != 0) {
                    PlayTypeKey originalPlayTypeKey = config.getPlayTypeKey();
                    for (PlayTypeKey key : config.getPlayTypeKeysForTraining()) {
                        config.setPlayTypeKey(key);
                        playGames(params.render, trainingStep);
                    }
                    config.setPlayTypeKey(originalPlayTypeKey);
                }

                log.info("counter: " + gameBuffer.getBuffer().getCounter());
                log.info("window size: " + gameBuffer.getBuffer().getWindowSize());

                log.info("gameBuffer size: " + this.gameBuffer.getBuffer().getGames().size());
            }

            modelService.trainModel().get();

            epoch = modelState.getEpoch();

            trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();

            duration.off();
            durations.add(duration);
            System.out.println("epoch;duration[ms];gpuMem[MiB]");
            IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));

        }

    }

    @SuppressWarnings("java:S106")
    public void train(TrainParams params) throws InterruptedException, ExecutionException {


        int trainingStep = 0;
        int epoch = 0;

        List<DurAndMem> durations = new ArrayList<>();

        loadBuffer(params.freshBuffer);
        initialFillingBuffer();

        modelService.loadLatestModelOrCreateIfNotExisting().get();
        epoch = modelState.getEpoch();
        int epochStart = epoch;



//        try (NDScope nDScope0 = new NDScope()) {
//            try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
//
//                Network network = new Network(config, model);
//                createNetworkModelIfNotExisting();

            //    epoch = NetworkHelper.getEpochFromModel(model);
             ///   int epochStart = epoch;

                while (trainingStep < config.getNumberOfTrainingSteps() &&
                        (config.getNumberOfEpisodesPerJVMStart() <= 0 || epoch - epochStart < config.getNumberOfEpisodesPerJVMStart())) {

                    DurAndMem duration = new DurAndMem();
                    duration.on();
                    int i = 0;

                    if (!params.freshBuffer) {
                        try (NDScope nDScope1 = new NDScope()) {
                            PlayTypeKey originalPlayTypeKey = config.getPlayTypeKey();
                            for (PlayTypeKey key : config.getPlayTypeKeysForTraining()) {
                                config.setPlayTypeKey(key);
                                playGamesOld(params.render,  trainingStep);
                            }
                            config.setPlayTypeKey(originalPlayTypeKey);
                        }

                        int n = gameBuffer.getBuffer().getGames().size();
                        int m = (int) gameBuffer.getBuffer().getGames().stream().filter(g ->
                                g.getGameDTO().getTStateB() != 0
                        ).count();
                        log.info("games with an alternative action " + m + " out of " + n);
                        log.info("counter: " + gameBuffer.getBuffer().getCounter());
                        log.info("window size: " + gameBuffer.getBuffer().getWindowSize());


                        log.info("gameBuffer size: " + this.gameBuffer.getBuffer().getGames().size());
                    }
                   // params.getAfterSelfPlayHookIn().accept(networkHelper.getEpoch(), network);
//                    try (NDScope nDScope1 = new NDScope()) {
//                        trainingStep = trainNetwork(model);
//                    }
                    //  modelService.trainModel().get();



                    modelService.trainModel().get();

                    epoch = modelState.getEpoch();

                    trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();

                    duration.off();
                    durations.add(duration);
                    System.out.println("epoch;duration[ms];gpuMem[MiB]");
                    IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));
                }
//            }
//        }

    }


    @SuppressWarnings("java:S106")
    public void trainOld(TrainParams params) throws InterruptedException, ExecutionException {

        int trainingStep = 0;
        int epoch = 0;

        List<DurAndMem> durations = new ArrayList<>();

        loadBuffer(params.freshBuffer);

        try (NDScope nDScope0 = new NDScope()) {
            try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
                Network network = new Network(config, model);
                if (!params.withoutFill) {
                    initialFillingBuffer(network);
                }
            }
        }
        try (NDScope nDScope0 = new NDScope()) {
            try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
                createNetworkModelIfNotExisting();
            }
        }

        try (NDScope nDScope0 = new NDScope()) {
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
                        try (NDScope nDScope1 = new NDScope()) {
                            PlayTypeKey originalPlayTypeKey = config.getPlayTypeKey();
                            for (PlayTypeKey key : config.getPlayTypeKeysForTraining()) {
                                config.setPlayTypeKey(key);
                                playGamesOld(params.render, trainingStep);
                            }
                            config.setPlayTypeKey(originalPlayTypeKey);
                        }

                        int n = gameBuffer.getBuffer().getGames().size();
                        int m = (int) gameBuffer.getBuffer().getGames().stream().filter(g ->
                                g.getGameDTO().getTStateB() != 0
                        ).count();
                        log.info("games with an alternative action " + m + " out of " + n);
                        log.info("counter: " + gameBuffer.getBuffer().getCounter());
                        log.info("window size: " + gameBuffer.getBuffer().getWindowSize());


                        log.info("gameBuffer size: " + this.gameBuffer.getBuffer().getGames().size());
                    }
                    params.getAfterSelfPlayHookIn().accept(networkHelper.getEpoch(), network);
                    try (NDScope nDScope1 = new NDScope()) {
                       // trainingStep = trainNetwork(model);
                    }
                    //  modelService.trainModel().get();



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

    public void playMultipleEpisodes(boolean render, boolean fastRuleLearning, boolean justInitialInferencePolicy) {
        List<Game> games = new ArrayList<>();
        List<Game> gamesToReanalyse = null;
        if (config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            gamesToReanalyse = gameBuffer.getGamesToReanalyse();
        }
        if (config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            games = multiGameStarter.reanalyseGames(config.getNumParallelGamesPlayed(),
                PlayParameters.builder()
                    .render(render)
                    .fastRulesLearning(fastRuleLearning)
                    .build(),
                gamesToReanalyse);
        } else {
            games = multiGameStarter.playNewGames(config.getNumParallelGamesPlayed(),
                PlayParameters.builder()
                    .render(render)
                    .fastRulesLearning(fastRuleLearning)
                    .build());
        }

        log.info("Played {} games parallel", games.size());

        gameBuffer.addGames2(games, false);
    }

}
