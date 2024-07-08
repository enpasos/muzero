package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.NDScope;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.Network;
import ai.enpasos.muzero.platform.agent.d_model.djl.*;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.IdProjection;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.TrainingDatasetType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.agent.d_model.djl.EpochHelper.getEpochFromModel;
import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.convertEpisodeDOsToGames;
import static ai.enpasos.muzero.platform.common.Constants.TRAIN_ALL;

@Component
@Slf4j
public class NetworkRulesTraining {

    @Autowired
    ModelQueue modelQueue;

    @Autowired
    MuZeroConfig config;
    @Autowired
    BatchFactory batchFactory;

    @Autowired
    TrainingConfigFactory trainingConfigFactory;
    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    private Helper helper;

    @Autowired
    private ModelState modelState;

    @Autowired
    private DBService dbService;

    @Autowired
    private EpisodeRepo episodeRepo;


    @Autowired
    private TimestepRepo timestepRepo;


    public void trainNetworkRules(Network network, boolean[] freeze, boolean background, TrainingDatasetType trainingDatasetType, int maxUnrollSteps) {
        List<Integer> uOKList = timestepRepo.uOkList();
        List<Integer> unrollStepsList = new ArrayList<>();
        for (int unrollSteps = 1; unrollSteps <= maxUnrollSteps; unrollSteps++) {
            if ((unrollSteps == 1 && (uOKList.contains(-2) || uOKList.contains(-1)|| uOKList.contains(0))) || unrollSteps > 1) {
                unrollStepsList.add(unrollSteps);
            }
        }
        for (int i = 0; i < unrollStepsList.size(); i++) {
            int unrollSteps = unrollStepsList.get(i);
            trainNetworkRulesOneUnrollStep(network, freeze, background, trainingDatasetType, unrollSteps, i != unrollStepsList.size() -1 );
        }
    }

    public void trainNetworkRulesOneUnrollStep(Network network, boolean[] freeze, boolean background, TrainingDatasetType trainingDatasetType, int unrollSteps, boolean saveVeto) {

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(8);

        boolean withSymmetryEnrichment = config.isWithSymmetryEnrichment();

        Model model = network.getModel();
        MuZeroBlock muZeroBlock = (MuZeroBlock) model.getBlock();
        muZeroBlock.setRulesModel(true);
        int epochLocal = getEpochFromModel(model);


        gameBuffer.resetRelevantIds();
        List<IdProjection> allIdProjections = gameBuffer.getRelevantIdsUOk(unrollSteps-1);


        List<Long> allRelevantTimestepIds = allIdProjections.stream().map(IdProjection::getId).toList();
        List<Long> allRelatedEpisodeIds = episodeIdsFromIdProjections(allIdProjections);

        log.info("allRelevantTimestepIds size: {}, allRelatedEpisodeIds size: {}", allRelevantTimestepIds.size(), allRelatedEpisodeIds.size());

        // start real code
        // first the buffer loop
        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        rulesBuffer.setIds(allRelatedEpisodeIds);
        log.info("unrollSteps: {}, relatedEpisodeIds size: {}", unrollSteps, rulesBuffer.getIds().size());
        int w = 0;

        System.out.println("epoch;unrollSteps;w;sumMeanLossL;sumMeanLossR;countNOK_0;countNOK_1;countNOK_2;countNOK_3;countNOK_4;countNOK_5;countNOK_6;count");
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> relatedEpisodeIds = iterator.next();
            Set<Long> relatedEpisodeIdsSet = new HashSet<>(relatedEpisodeIds);
            log.info("timestep before relatedTimeStepIds filtering");
            Set<Long> relatedTimeStepIds = allIdProjections.stream()
                    .filter(idProjection -> relatedEpisodeIdsSet.contains(idProjection.getEpisodeId()))
                    .map(IdProjection::getId).collect(Collectors.toSet());
            log.info("timestep after relatedTimeStepIds filtering");

            boolean save = !iterator.hasNext() && !saveVeto;
            log.info("epoch: {}, unrollSteps: {}, w: {}, save: {}", epochLocal, unrollSteps, w, save);
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(relatedEpisodeIds);
            List<Game> gameBuffer = convertEpisodeDOsToGames(episodeDOList, config);
            Collections.shuffle(gameBuffer);

            // each timestep once


            List<TimeStepDO> allTimeSteps = allRelevantTimeStepsShuffled3(gameBuffer, relatedTimeStepIds);

            log.info("epoch: {}, unrollSteps: {},  allTimeSteps.size(): {}", epochLocal, unrollSteps, allTimeSteps.size());


            muZeroBlock.setNumUnrollSteps(unrollSteps);

            Shape[] inputShapes = batchFactory.getInputShapesForRules(unrollSteps);


            try (NDScope nDScope = new NDScope()) {


                DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epochLocal, save, background, config.isWithConsistencyLoss(), true, unrollSteps);
                int finalEpoch = epochLocal;
                djlConfig.getTrainingListeners().stream()
                        .filter(MyEpochTrainingListener.class::isInstance)
                        .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(finalEpoch));

                try (Trainer trainer = model.newTrainer(djlConfig)) {
                    trainer.setMetrics(new Metrics());
                    trainer.initialize(inputShapes);
                    ((DCLAware) model.getBlock()).freezeParameters(freeze);


                    for (int ts = 0; ts < allTimeSteps.size(); ts += config.getBatchSize()) {
                        List<TimeStepDO> batchTimeSteps = allTimeSteps.subList(ts, Math.min(ts + config.getBatchSize(), allTimeSteps.size()));
                        trainBatch(unrollSteps, batchTimeSteps, trainer, withSymmetryEnrichment, epochLocal, w, nf);
                    }
                    if (!background) {
                        helper.handleMetrics( trainer, model, epochLocal);
                    }
                    trainer.notifyListeners(listener -> listener.onEpoch(trainer));
                }
                epochLocal = getEpochFromModel(model);
                modelState.setEpoch(epochLocal);
            }
            w++;
        }
    }

    private void trainBatch(int unrollSteps, List<TimeStepDO> batchTimeSteps, Trainer trainer, boolean withSymmetryEnrichment, int epochLocal, int w, NumberFormat nf) {
        try (Batch batch = batchFactory.getRulesBatchFromBuffer(batchTimeSteps, trainer.getManager(), withSymmetryEnrichment, unrollSteps)) {
            Statistics stats = new Statistics();
            List<EpisodeDO> episodes = batchTimeSteps.stream().map(ts_ -> ts_.getEpisode()).toList();  // TODO simplify

            int[] from = batchTimeSteps.stream().mapToInt(ts_ -> ts_.getT()).toArray();

            boolean[][][] b_OK_batch = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodes);
            MyEasyTrainRules.trainBatch(trainer, batch, b_OK_batch, from, stats);

            // transfer b_OK back from batch array to the games parameter s
            //  ZipperFunctions.sandu_in_Episodes_From_b_OK(b_OK_batch, episodes );
            ZipperFunctions.sandu_in_Timesteps_From_b_OK(b_OK_batch, episodes, batchTimeSteps);

            batchTimeSteps.stream().forEach(timeStepDO -> timeStepDO.setUOkTested(true));
            dbService.updateEpisodes_SandUOkandBox(episodes, unrollSteps);


            int tau = 0;   // start with tau = 0
            int[] countNOK = new int[9];
            for (int i = 0; i < countNOK.length; i++) {
                countNOK[i] = countNOKFromB_OK(b_OK_batch, i);
            }
            int count = stats.getCount();
            double sumLossR = stats.getSumLossReward();
            double sumMeanLossR = sumLossR / count;
            double sumLossL = stats.getSumLossLegalActions();
            double sumMeanLossL = sumLossL / count;

// Build the output string
            String output = epochLocal + ";" + unrollSteps + ";" + w + ";" + nf.format(sumMeanLossL) + ";" + nf.format(sumMeanLossR);
            for (int n : countNOK) {
                output += ";" + n;
            }
            output += ";" + count;
            System.out.println(output); trainer.step();
        }
    }


    private List<Long> episodeIdsFromIdProjections(  List<IdProjection> allIdProjections) {
        Set<Long> ids =  allIdProjections.stream().mapToLong(p -> p.getEpisodeId())
                .boxed().collect(Collectors.toSet());
        return new ArrayList(ids);
    }

    private List<Long> episodeIdsFromTimestepIds(Map<Long, Long> timestepIdToEpisodeId,   List<Long> timestepIdsRulesLearningList) {
        Set<Long> ids =  timestepIdsRulesLearningList.stream().mapToLong(timestepId -> timestepIdToEpisodeId.get(timestepId))
                .boxed().collect(Collectors.toSet());
        return new ArrayList(ids);
    }


    private int countNOKFromB_OK(boolean[][][] bOkBatch, int tau) {
        int countNOK = 0;
        for (int e = 0; e < bOkBatch.length; e++) {


            for (int i = 0; i < bOkBatch[e].length; i++) { // from
                int j = i + tau;
                if (j < bOkBatch[e][i].length) {
                    // for (int j = i; j < bOkBatch[e][i].length; j++) { // to
                    if (!bOkBatch[e][i][j]) {
                        countNOK++;
                    }
                }
                //  }
            }
        }
        return countNOK;
    }
    private List<TimeStepDO> allTimeStepsShuffled(List<Game> games) {
        List<TimeStepDO> timeStepDOList = new ArrayList<>();
        for (Game game : games) {
            EpisodeDO episodeDO = game.getEpisodeDO();
            for (TimeStepDO ts : episodeDO.getTimeSteps()) {
                timeStepDOList.add(ts);
            }
        }
        Collections.shuffle(timeStepDOList);
        return timeStepDOList;
    }


    private List<TimeStepDO> allRelevantTimeStepsShuffled(List<Game> games, List<Integer> boxesRelevant) {
        List<TimeStepDO> timeStepDOList = new ArrayList<>();
        for (Game game : games) {
            EpisodeDO episodeDO = game.getEpisodeDO();
            for (TimeStepDO ts : episodeDO.getTimeSteps()) {
                if (boxesRelevant.contains(ts.getBox())) {
                    timeStepDOList.add(ts);
                }
            }
        }
        Collections.shuffle(timeStepDOList);
        return timeStepDOList;
    }

    private List<TimeStepDO> allRelevantTimeStepsShuffled2(List<Game> games , int uOk) {
        List<TimeStepDO> timeStepDOList = new ArrayList<>();
        for (Game game : games) {
            EpisodeDO episodeDO = game.getEpisodeDO();
            for (TimeStepDO ts : episodeDO.getTimeSteps()) {
                if (!ts.isUOkClosed() && ts.getUOk() == uOk) {
                    timeStepDOList.add(ts);
                }
            }
        }
        Collections.shuffle(timeStepDOList);
        return timeStepDOList;
    }


    private List<TimeStepDO> allRelevantTimeStepsShuffled3(List<Game> games , Set<Long> timestepIds) {
        List<TimeStepDO> timeStepDOList = new ArrayList<>();
        for (Game game : games) {
            EpisodeDO episodeDO = game.getEpisodeDO();
            for (TimeStepDO ts : episodeDO.getTimeSteps()) {
                if (timestepIds.contains(ts.getId())) {
                    timeStepDOList.add(ts);
                }
            }
        }
        Collections.shuffle(timeStepDOList);
        return timeStepDOList;
    }


}
