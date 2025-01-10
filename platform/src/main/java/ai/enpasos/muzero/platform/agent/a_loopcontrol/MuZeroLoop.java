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

package ai.enpasos.muzero.platform.agent.a_loopcontrol;


import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.b_episode.Play;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.RuleBufferService;
import ai.enpasos.muzero.platform.agent.e_experience.TestEpisodesForRulesTraining;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import ai.enpasos.muzero.platform.run.TestUnrollRulestate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import ai.enpasos.muzero.platform.agent.e_experience.box.Boxing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.config.TrainingDatasetType.PLANNING_BUFFER;

@Slf4j
@Component
public class MuZeroLoop {

    @Autowired
    MuZeroConfig config;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    ModelService modelService;

    @Autowired
    Play play;

    @Autowired
    ModelState modelState;



    @Autowired
    EpisodeRepo episodeRepo;

    @Autowired
    TimestepRepo timestepRepo;


    @Autowired
    PlayService playService;

    @Autowired
    TestUnrollRulestate testUnrollRulestate;

    @Autowired
    TestEpisodesForRulesTraining testEpisodesForRulesTraining;

    @Autowired
    DBService dbService;

    @Autowired
    RuleBufferService ruleBufferService;


    @SuppressWarnings("java:S106")
    public void train(TrainParams params) throws InterruptedException, ExecutionException {


        boolean ok = false;
        int unrollSteps = 1;
        while (!ok) {
            trainRules2(unrollSteps );

            // for now increase unrollSteps if rulesBuffer is not completely filled
            if (!gameBuffer.areThereTimeStepsToBeTrainedAndNokInRulesBuffer() || !gameBuffer.getRulesBuffer().isBufferFilled()) {
                if (config.getMaxUnrollSteps() == unrollSteps) {
                    log.info("maxUnrollSteps reached ... ");
                    return;
                }
                log.info("all timesteps in buffer are ok ... ");
                unrollSteps++;
            }
          //  trainRules();
          //  ok = trainPolicyAndValue(params);
        }

        log.info("done");
    }

    private void trainRules2(int unrollSteps ) throws InterruptedException, ExecutionException  {
        int epoch;
        int trainingStep;
        boolean policyValueTraining = true;   // true: policy and value training, false: rules training
        boolean  rulesTraining = false;
        List<DurAndMem> durations = new ArrayList<>();
        modelService.loadLatestModelOrCreateIfNotExisting().get();

        epoch = modelState.getEpoch();
        trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();

        if (episodeRepo.count() < config.getInitialRandomEpisodes()) {
            play.randomEpisodes(config.getInitialRandomEpisodes() - (int) episodeRepo.count());
        }

        gameBuffer.clearShortObjectsCache();


        log.info("fillRuleBufferFromDB ... ");
        gameBuffer.fillRuleBufferFromDB(10000);



     //   ruleBufferService.run();
      //  int unrollSteps = 1;   // just an example
      //  testUnrollRulestate.testForEpisodeId(epoch, unrollSteps,   2082001L);  // just for testing

        testRulesBuffer(unrollSteps, epoch);
        // List<Game> bufferGames = gameBuffer.getRulesBuffer().getEpisodeMemory().getGameList();


        //Collections.shuffle(bufferGames);

        int numEpisodesToTest = config.getNumParallelGamesPlayed();
        boolean start = true;

        int c = 0;
        int numEpisodesInRulesBuffer = gameBuffer.getRulesBuffer().getEpisodeMemory().getNumberOfEpisodes();
        while (testEpisodesForRulesTraining.test(unrollSteps, numEpisodesToTest, start, epoch)) {
            start = false;
            epoch = modelState.getEpoch();
            log.info("testEpisodesForRulesTraining.test() ... , count = {}", c++);
            if (gameBuffer.getRulesBuffer().isBufferFilled() || numEpisodesInRulesBuffer == gameBuffer.getRulesBuffer().getEpisodeMemory().getNumberOfEpisodes()) {
                log.info("ruleTrain2 ... ");
                ruleTrain2(durations, unrollSteps);
                testRulesBuffer(unrollSteps, epoch);
            }
            numEpisodesInRulesBuffer = gameBuffer.getRulesBuffer().getEpisodeMemory().getNumberOfEpisodes();
        }
  //      if (!gameBuffer.getRulesBuffer().isBufferFilled()) {
   //         throw new RuntimeException("ruleTrain2 ... rulesBuffer not filled ... t.b.d.");

//            ruleTrain2(durations, unrollSteps);
//            testRulesBuffer(unrollSteps, epoch);
       // }


    }

    private void testRulesBuffer(int unrollSteps, int epoch ) {
        List<Game> bufferGames = gameBuffer.getRulesBuffer().getEpisodeMemory().getGameList();
        log.info("testEpisodesForRulesTraining.analyseGames in rulesBuffer ... ");
        testEpisodesForRulesTraining.analyseGames(unrollSteps, epoch, bufferGames);
        testEpisodesForRulesTraining.logStatisticalInfoAboutGames("ruleBuffer", bufferGames, unrollSteps, epoch);

        // first remove from rulesBuffer all games with no timesteps to be trained then from the copied list
        bufferGames.forEach(g -> {
            if (g.getEpisodeDO().getTimeSteps().stream().noneMatch(TimeStepDO::isToBeTrained)) {
                gameBuffer.getRulesBuffer().getEpisodeMemory().remove(g);
            }
        });
    }

//    private int groupingForRulesTraining(List<DurAndMem> durations, List<Game> bufferGames, int unrollSteps, int dn) throws InterruptedException, ExecutionException {
//
//
//        List<Game> gamesToTrain = bufferGames.stream().filter(Game::isRulesTraining).collect(Collectors.toList());
//        List<Game> nonTrainedGames = bufferGames.stream().filter(g -> !g.isRulesTraining()).collect(Collectors.toList());
//
//        // modelService.loadLatestModelOrCreateIfNotExisting().get();
//        log.debug("load latest model .... ");
//        modelService.loadLatestModel().get();
//
//
//        uOkAnalyseGames(durations, bufferGames, unrollSteps);
//
//        bufferGames.forEach(g -> g.getEpisodeDO().getTimeSteps().forEach(TimeStepDO::memorizeNormedSampleError));
//
//        log.debug("ruleTrain2 ... ");
//        ruleTrain2(durations, unrollSteps);
//
//        uOkAnalyseGames(durations, bufferGames, unrollSteps);
//
//        // set maxSampleErrorChange from all time steps
//        nonTrainedGames.forEach(g -> {
//            double maxSampleErrorChange = g.getEpisodeDO().getTimeSteps().stream().mapToDouble(TimeStepDO::getSampleErrorChange).max().orElse(0.0);
//            g.setMaxSampleErrorChange(maxSampleErrorChange);
//        });
//
//        List<Game> criticalNonTrainedGames = nonTrainedGames.stream().filter(g -> g.getMaxSampleErrorChange() > 0.001d).collect(Collectors.toList());
//        // sort nonTrainedGames by maxSampleErrorChange
//        criticalNonTrainedGames.sort((g1, g2) -> {
//            return Double.compare(g2.getMaxSampleErrorChange(), g1.getMaxSampleErrorChange());
//        });
//        if (!criticalNonTrainedGames.isEmpty()) {
//            criticalNonTrainedGames.getFirst().setRulesTraining(true);
//            gamesToTrain.add(criticalNonTrainedGames.getFirst());
//            log.info("sample error change: {}", criticalNonTrainedGames.getFirst().getMaxSampleErrorChange());
//        }
//
//        // get nonTrainedGames with maxSampleErrorChange > 0
////
////        Collections.shuffle(criticalNonTrainedGames);
//        log.info("gamesToTrain: {}, criticalNonTrainedGames: {}", gamesToTrain.size(), criticalNonTrainedGames.size());
////        criticalNonTrainedGames.subList(0, Math.min(dn, criticalNonTrainedGames.size())).forEach(g -> g.setRulesTraining(true));
//
//        return criticalNonTrainedGames.size();
//   //     return 10;
//
////        // sort nonTrainedGames by sampleErrorChange
////        nonTrainedGames.sort((g1, g2) -> {
////            return Double.compare(g1.getMaxSampleErrorChange(), g2.getMaxSampleErrorChange());
////        });
////
////        // select the 100 gamesToTrain with the highest sampleErrorChange and add them to gamesToTrain
////        gamesToTrain.addAll(nonTrainedGames.subList(0, dn));
////
////        nonTrainedGames.forEach(g -> g.setRulesTraining(false));
////        gamesToTrain.forEach(g -> g.setRulesTraining(true));
//    }

    private void uOkAnalyseGames(List<DurAndMem> durations, List<Game> bufferGames, int unrollSteps) {
        DurAndMem duration = new DurAndMem();
        duration.on();
        log.debug("uOkAnalyseGames ... ");
        // batch bufferGames to 1000 batches
        List<List<Game>> batches = new ArrayList<>();
        int batchSize = 1000;
        for (int i = 0; i < bufferGames.size(); i += batchSize) {
            batches.add(bufferGames.subList(i, Math.min(i + batchSize, bufferGames.size())));
        }
        batches.forEach(batch -> playService.uOkAnalyseGames(batch,  false, unrollSteps, true));
      //  playService.uOkAnalyseGames(bufferGames,  false, unrollSteps, true);
        duration.off();
        durations.add(duration);
        System.out.println("epoch;duration[ms];gpuMem[MiB]");
        IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));
    }

    private boolean trainPolicyAndValue(TrainParams params) throws InterruptedException, ExecutionException {
        int epoch;
        int trainingStep;
      //  boolean policyValueTraining = true;   // true: policy and value training, false: rules training
        boolean  rulesTraining = false;
        List<DurAndMem> durations = new ArrayList<>();

        modelService.loadLatestModelOrCreateIfNotExisting().get();
        epoch = modelState.getEpoch();
        trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();

        gameBuffer.fillPlanningBufferFromDB();

        while (trainingStep < config.getNumberOfTrainingSteps()) {

            DurAndMem duration = new DurAndMem();
            duration.on();

        //    if (policyValueTraining) {
                if (epoch != 0) {
                    PlayTypeKey originalPlayTypeKey = config.getPlayTypeKey();
                    for (PlayTypeKey key : config.getPlayTypeKeysForTraining()) {
                        config.setPlayTypeKey(key);
                        play.playGames(params.isRender(), trainingStep);
                    }
                    config.setPlayTypeKey(originalPlayTypeKey);
                }

                log.info("game counter: " + gameBuffer.getPlanningBuffer().getCounter());
                log.info("window size: " + gameBuffer.getPlanningBuffer().getWindowSize());
                log.info("gameBuffer size: " + this.gameBuffer.getPlanningBuffer().getEpisodeMemory().getGameList().size());
      //      }

      //      boolean[] freeze = null;

     //       if (policyValueTraining) {
            boolean[]   freeze = new boolean[]{true, false, false};
            modelService.trainModel(freeze, PLANNING_BUFFER, false).get();

            gameBuffer.clearShortObjectsCache();
            testUnrollRulestate.testNewEpisodes();

            gameBuffer.checkEpisodesOkAndUpdateIfChanged(epoch);
            long nEpisodesNotOK = episodeRepo.countEpisodesWithOkFalse();
            log.info("nEpisodesNotOK: {}", nEpisodesNotOK);
            if (nEpisodesNotOK > 0) {
                return false;
            }
      //      }

            epoch = modelState.getEpoch();
            trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();

            duration.off();
            durations.add(duration);
            System.out.println("epoch;duration[ms];gpuMem[MiB]");
            IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));
        }
        return true;
    }

    private void trainRules() throws InterruptedException, ExecutionException {
        int trainingStep = 0;
        int epoch = 0;
        int nTrain = config.getNumberOfTrainingSamplesPerRuleTrainingEpoch();

        gameBuffer.clearShortObjectsCache();


        List<DurAndMem> durations = new ArrayList<>();

        modelService.loadLatestModelOrCreateIfNotExisting().get();
        epoch = modelState.getEpoch();


        // just for testing
        //episodeRepo.deleteEpisodesNotOk();

     //   gameBuffer.checkEpisodesOkAndUpdateIfNot(epoch);


        if (episodeRepo.count() < config.getInitialRandomEpisodes()) {
            play.randomEpisodes(config.getInitialRandomEpisodes() - (int) episodeRepo.count());
        }

        gameBuffer.clearShortObjectsCache();

        testUnrollRulestate.testNewEpisodes();

        int unrollSteps = gameBuffer.findStartUnrollSteps(epoch) ;

        int loopCounter = 0;

        log.info("unrollSteps: {} ... about to enter the Leithner training loop", unrollSteps);
        while (!gameBuffer.everthingKnown(epoch)  && trainingStep < config.getNumberOfTrainingSteps()) {
            if (loopCounter > 0) {

                gameBuffer.checkEpisodesOkAndUpdateIfChanged(epoch);

                // do the testing with Leithner's selection of samples
                if (Boxing.isUsed(Boxing.MAX_BOX, epoch) || loopCounter == 1) {
                    // we simply test everything
                    testUnrollRulestate.test();
                } else {
                    testUnrollRulestate.testAllButBoxPropagationOnlyForRelevantTimesteps(epoch, unrollSteps);
                }


                if (gameBuffer.everthingKnown(unrollSteps, epoch)) {
                    unrollSteps = unrollSteps + 1;
                    log.info("unrollSteps increased to {}", unrollSteps);
                }
                if (unrollSteps == config.getMaxUnrollSteps() && gameBuffer.everthingKnown(epoch)) {
                    testUnrollRulestate.test();
                    if (gameBuffer.everthingKnown(epoch)) {
                        log.info("everything known");
                        break;
                    }
                }
            }


           // do the training with Leithner's selection of samples
           // select from box 0 ... box MAX_BOX-1
            epoch = ruleTrain(durations, unrollSteps );

            loopCounter++;

        }
    }



    private void logStateInfo(int unrollSteps, int epoch) {
        log.info("numBox0({}) = {}",unrollSteps, numBox0(unrollSteps ));
        log.info("num closed episodes: {}", gameBuffer.numClosedEpisodes(epoch));
        gameBuffer.selectUnrollStepsToEpisodeCount(true, epoch);
    }

    private int getNOpen(int epoch) {
        return gameBuffer.numEpisodes(epoch) - gameBuffer.numClosedEpisodes(epoch);
    }

    private int ruleTrain(  List<DurAndMem> durations, int unrollSteps  ) throws InterruptedException, ExecutionException {
        int epoch;
        DurAndMem duration = new DurAndMem();
        duration.on();
        boolean[] freeze = new boolean[]{false, true, true};
        modelService.trainModelRules(freeze , unrollSteps  ).get();

        epoch = modelState.getEpoch();
        duration.off();
        durations.add(duration);
        System.out.println("epoch;duration[ms];gpuMem[MiB]");
        IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));
        return epoch;
    }

    private int ruleTrain2( List<DurAndMem> durations, int unrollSteps ) throws InterruptedException, ExecutionException {
        int epoch;
        DurAndMem duration = new DurAndMem();
        duration.on();
        boolean[] freeze = new boolean[]{false, true, true};
     //   modelService.loadLatestModel().get();
        modelService.trainModelRules2(freeze , unrollSteps   ).get();
        epoch = modelState.getEpoch();
        duration.off();
        durations.add(duration);
        System.out.println("epoch;duration[ms];gpuMem[MiB]");
        IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));
        return epoch;
    }



//    private long numNotClosed() {
//        long numNotClosed = timestepRepo.numNotClosed();
//        log.info("numNotClosed: {}",  numNotClosed);
//        return numNotClosed;
//    }


    private int numBox0(int unrollSteps ) {
        int n = gameBuffer.getShortTimestepSetFromCacheFillCacheIfEmpty( ).stream().filter(t -> t.getBox(unrollSteps) == 0).mapToInt(t -> t.getBoxes().length).sum();
        log.info("numBox0 = {}", n);
        return n;
    }




}
