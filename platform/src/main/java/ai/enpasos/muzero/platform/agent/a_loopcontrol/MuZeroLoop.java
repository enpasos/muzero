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


import ai.enpasos.muzero.platform.agent.b_episode.Play;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import ai.enpasos.muzero.platform.run.TestUnrollRulestate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    TestUnrollRulestate testUnrollRulestate;

    @Autowired
    DBService dbService;


    @SuppressWarnings("java:S106")
    public void train(TrainParams params) throws InterruptedException, ExecutionException {

        trainRules();

        trainPolicyAndValue(params);

        log.info("done");
    }

    private void trainPolicyAndValue(TrainParams params) throws InterruptedException, ExecutionException {
        int epoch;
        int trainingStep;
        boolean policyValueTraining = true;   // true: policy and value training, false: rules training
        boolean  rulesTraining = false;
        List<DurAndMem> durations = new ArrayList<>();

        modelService.loadLatestModelOrCreateIfNotExisting().get();
        epoch = modelState.getEpoch();
        trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();

        gameBuffer.loadLatestStateIfExists();




        while (trainingStep < config.getNumberOfTrainingSteps()) {

            DurAndMem duration = new DurAndMem();
            duration.on();

            if (policyValueTraining) {
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
            }

            boolean[] freeze = null;

            if (policyValueTraining) {
                freeze = new boolean[]{true, false, false};
                modelService.trainModel(freeze, PLANNING_BUFFER, false).get();
            }

            epoch = modelState.getEpoch();
            trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();

            duration.off();
            durations.add(duration);
            System.out.println("epoch;duration[ms];gpuMem[MiB]");
            IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));
        }
    }

    private void trainRules() throws InterruptedException, ExecutionException {
        int trainingStep = 0;
        int epoch = 0;
        int nTrain = config.getNumberOfTrainingSamplesPerRuleTrainingEpoch();

        List<DurAndMem> durations = new ArrayList<>();

        modelService.loadLatestModelOrCreateIfNotExisting().get();
        epoch = modelState.getEpoch();

        if (episodeRepo.count() < config.getInitialRandomEpisodes()) {
            play.randomEpisodes(config.getInitialRandomEpisodes() - (int) episodeRepo.count());
        }

        gameBuffer.clearEpisodeIds();

        testUnrollRulestate.testNewEpisodes();

        int unrollSteps = gameBuffer.findStartUnrollSteps() ;

        while (getNOpen() > 0 && trainingStep < config.getNumberOfTrainingSteps()) {
            int numBox0 = numBox0(unrollSteps);
            while (numBox0 > 0) {

                logStateInfo(unrollSteps);
//                if (getNOpen() < nTrain) {
//                    testUnrollRulestate.test();
//                } else {
                    testUnrollRulestate.testEpisodesThatNeedTo();  // the full testing triggered by change in unrollSteps
                    logStateInfo(unrollSteps);
                    testUnrollRulestate.identifyRelevantTimestepsAndTestThem(epoch); // test box and epoch triggered testing
              //  }
                logStateInfo(unrollSteps);

                epoch = ruleTrain(durations, unrollSteps );

                numBox0 = numBox0(unrollSteps);
                if (gameBuffer.numNeedsTrainingPrio1( unrollSteps) == 0) {
                    testUnrollRulestate.test();
                   // if (gameBuffer.numNeedsTrainingPrio1( unrollSteps) == 0) {
                        unrollSteps = unrollSteps + 1;
                        log.info("unrollSteps increased to {}", unrollSteps);
                        numBox0 = numBox0(unrollSteps);
                 //   }
                }
            }



            if (getNOpen() == 0) {
                break;
            }


        }
    }

    private void logStateInfo(int unrollSteps) {
        log.info("numBox0({}) = {}",unrollSteps, numBox0(unrollSteps));
        log.info("num closed episodes: {}", gameBuffer.numClosedEpisodes());
        gameBuffer.selectUnrollStepsToEpisodeCount(true);
    }

    private int getNOpen() {
        return gameBuffer.numEpisodes() - gameBuffer.numClosedEpisodes();
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



//    private long numNotClosed() {
//        long numNotClosed = timestepRepo.numNotClosed();
//        log.info("numNotClosed: {}",  numNotClosed);
//        return numNotClosed;
//    }


    private int numBox0(int unrollSteps) {
        int n = gameBuffer.getShortTimestepSet().stream().filter(t -> t.getBox(unrollSteps) == 0).mapToInt(t -> t.getBoxes().length).sum();
        log.info("numBox0 = {}", n);
        return n;
    }




}
