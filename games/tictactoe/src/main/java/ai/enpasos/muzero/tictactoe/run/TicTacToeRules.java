package ai.enpasos.muzero.tictactoe.run;


import ai.djl.Device;
import ai.djl.Model;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.NDScope;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.agent.d_model.Network;
import ai.enpasos.muzero.platform.agent.d_model.Sample;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyEasyTrain;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyEpochTrainingListener;
import ai.enpasos.muzero.platform.agent.d_model.djl.TrainingConfigFactory;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.MuZeroNoSampleMatch;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.d_model.djl.EpochHelper.getEpochFromModel;
import static ai.enpasos.muzero.platform.common.FileUtils.mkDir;
import static ai.enpasos.muzero.platform.config.PlayTypeKey.PLAYOUT;
import static java.util.Map.entry;

@Slf4j
@Component
public class TicTacToeRules {

    @Autowired
    Inference inference;


    @Autowired
    ModelService modelService;

    @Autowired
    MuZeroConfig config;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    EpisodeRepo episodeRepo;

    public void run() {


        int pageSize = 200;

        episodeRepo.initRuleLoss();

     //   List<MyLoss> result = new ArrayList<>();

        // load model (see ModelController)
        Model model = Model.newInstance(config.getModelName(), Device.gpu());
        if (model.getBlock() == null) {
            MuZeroBlock block = new MuZeroBlock(config);
            model.setBlock(block);


            loadModelParametersOrCreateIfNotExisting(model);

        }
        Network network = new Network(config, model);
        NDManager nDManager = network.getNDManager().newSubManager();
        network.initActionSpaceOnDevice(nDManager);
        network.setHiddenStateNDManager(nDManager);


        Pair<List<Game>, Integer> gamesAndTotalPagesCounter = gameBuffer.getGamesByPage(0, pageSize);
        List<Game> games =  gamesAndTotalPagesCounter.getLeft();
        games.forEach(g -> g.setForRulesTrainingOnly(true));
        int totalPages = gamesAndTotalPagesCounter.getRight();


        List<MyLoss> result2 = new ArrayList<>();
        analyseGames(model, games , result2);
        result2 = keepLargestLossEntriesOnly(result2);
        result2.forEach(myLoss -> episodeRepo.updateRuleLoss(myLoss.getId(), myLoss.getLoss()));


        for (int page = 1; page < totalPages; page++) {
            log.info("page: " + page + " of " + totalPages );
            gamesAndTotalPagesCounter = gameBuffer.getGamesByPage(page, pageSize);
            games =  gamesAndTotalPagesCounter.getLeft();
             result2 = new ArrayList<>();
            analyseGames(model, games , result2);
            result2 = keepLargestLossEntriesOnly(result2);
            result2.forEach(myLoss -> episodeRepo.updateRuleLoss(myLoss.getId(), myLoss.getLoss()));

           // result.addAll( result2);
        }



//        result.sort(Comparator.comparing(MyLoss::getLoss));
//        Collections.reverse(result);
//
//        // print the first 100 entries of result
//        for (int i = 0; i < 100; i++) {
//            MyLoss myLoss = result.get(i);
//            log.info("loss: {}, id: {}, t: {}, tmax: {}", myLoss.getLoss(), myLoss.getId(), myLoss.getT(), myLoss.getTmax());
//        }
//
//        result.forEach(myLoss -> episodeRepo.updateRuleLoss(myLoss.getId(), myLoss.getLoss()));

    }

    @NotNull
    private static List<MyLoss> keepLargestLossEntriesOnly(List<MyLoss> result) {
        Map<Long, List<MyLoss>> map = result.stream().collect(Collectors.groupingBy(MyLoss::getId));
        result = new ArrayList<>();
        for (Map.Entry<Long, List<MyLoss>> entry : map.entrySet()) {
            List<MyLoss> myLosses = entry.getValue();
            myLosses.sort(Comparator.comparing(MyLoss::getLoss));
            Collections.reverse(myLosses);
            result.add(myLosses.get(0));
        }
        return result;
    }

    private void analyseGames(Model model, List<Game> games,   List<MyLoss> result) {


        int epoch = getEpochFromModel(model);
        //  create samples
        List<Sample>  samples = new ArrayList<>();
        int numUnrollSteps = config.getNumUnrollSteps();
        try (NDManager ndManager = NDManager.newBaseManager(Device.cpu())) {
            for (Game game : games) {
                int t0 = Math.max(0, game.getEpisodeDO().getLastTime() - numUnrollSteps);
                for (int t = t0; t <= game.getEpisodeDO().getLastTime(); t++) {
                    Sample sample = gameBuffer.sampleFromGame(numUnrollSteps,  game, t);
                    samples.add(sample);
                }
            }

        }



        // evaluate samples
        try (NDScope nDScope = new NDScope()) {

            int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
            boolean withSymmetryEnrichment = false;

            DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epoch);

            djlConfig.getTrainingListeners().stream()
                    .filter(MyEpochTrainingListener.class::isInstance)
                    .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(epoch));
            try (Trainer trainer = model.newTrainer(djlConfig)) {
                Shape[] inputShapes = batchFactory.getInputShapes();
                trainer.initialize(inputShapes);
                trainer.setMetrics(new Metrics());

                try (Batch batch = batchFactory.getBatch(trainer.getManager(), withSymmetryEnrichment, samples)) {
                    NDArray losses = MyEasyTrain.validateBatch(trainer, batch, false, config.withLegalActionsHead());
                    for (int s = 0; s < samples.size(); s++) {
                        long id = samples.get(s).getGame().getEpisodeDO().getId();
                        float loss = losses.getFloat(s);
                        MyLoss myLoss = MyLoss.builder()
                                .id(id)
                                .loss(loss)
                                .t(samples.get(s).getGamePos())
                                .tmax(samples.get(s).getGame().getEpisodeDO().getLastTime())
                                .build();
                        result.add(myLoss);
                    }
                }
            }
        }
    }


    @Data
    @Builder
    static class MyLoss {
        public float loss;
        public long id;
        public int t;
        public int tmax;
    }




    private void loadModelParametersOrCreateIfNotExisting(Model model) {
        try {
            String outputDir = config.getNetworkBaseDir();
            mkDir(outputDir);
            model.load( Paths.get(outputDir));
        } catch (Exception e) {

            final int epoch = -1;
            DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epoch);

            djlConfig.getTrainingListeners().stream()
                    .filter(MyEpochTrainingListener.class::isInstance)
                    .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(epoch));
            try (Trainer trainer = model.newTrainer(djlConfig)) {
                Shape[] inputShapes = batchFactory.getInputShapes();
                trainer.initialize(inputShapes);
                trainer.notifyListeners(listener -> listener.onEpoch(trainer));
            }
        }


        long numberOfParameters = model.getBlock().getParameters().stream().mapToLong(p -> p.getValue().getArray().size()).sum();
        log.info("{} parameters in the model.", numberOfParameters);

    }

    @Autowired
    BatchFactory batchFactory;

    @Autowired
    TrainingConfigFactory trainingConfigFactory;
}
