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
import ai.enpasos.muzero.platform.run.FillRulesLoss;
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
    FillRulesLoss fillRulesLoss;

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

        testUnrollRulestate.testNewEpisodes( );  //test( );

//        int unrollSteps = 1;
//        try {
//            unrollSteps = Math.max(1, timestepRepo.minUokNotClosed() + 1);
//        } catch (Exception e) {
//            unrollSteps = config.getMaxUnrollSteps();
//        }
        //      dbService.updateBox0(unrollSteps);
        //     log.info("unrollSteps: {}", unrollSteps);
        dbService.setNextuoktarget(config.getMaxUnrollSteps());

        long numBoxA0 = numBoxA0();
        long numBoxB0 = numBoxB0();

        int hyperEpoch = 0;

        while (numBoxA0 != 0 || numBoxB0 != 0) {
            // while (unrollSteps <= config.getMaxUnrollSteps() && trainingStep < config.getNumberOfTrainingSteps()) {
            // log.info("minUnrollSteps: {} <= maxUnrollSteps: {}", unrollSteps, config.getMaxUnrollSteps());
            while (numBoxA0 > 0) {
                int unrollSteps = 1;
                log.info("numBoxA0: {}", numBoxA0);
                if (numBoxA0 < nTrain) {
                    testUnrollRulestate.identifyRelevantTimestepsAndTestThemA(unrollSteps, epoch);
                }
                epoch = ruleTrain(unrollSteps, durations);
                numBoxA0 = numBoxA0();

            }
            numBoxB0 = numBoxB0();
            hyperEpoch++;
            log.info("numBoxB0: {}, hyperEpoch: {}", numBoxB0, hyperEpoch);
            int unrollSteps = config.getMaxUnrollSteps();
            if (numBoxB0 < nTrain) {
                testUnrollRulestate.identifyRelevantTimestepsAndTestThemB(unrollSteps, hyperEpoch);
            }
         //   modelState.setHyperepoch(hyperEpoch);
            epoch = ruleTrain(unrollSteps, durations);

            numBoxA0 = numBoxA0();
            numBoxB0 = numBoxB0();

            if (numBoxA0 == 0 && numBoxB0 == 0) {
                log.info("numBoxA0 == 0 && numBoxB0 == 0");
                // log.info("firstBoxes == 0; unrollSteps: {}; maxUnrollSteps: {}", unrollSteps, config.getMaxUnrollSteps());

                testUnrollRulestate.test(config.getMaxUnrollSteps());
                numBoxA0 = numBoxA0();
                numBoxB0 = numBoxB0();


                if (numBoxA0 == 0 && numBoxB0 == 0) {
                    log.info("after testing: numBoxA0 == 0 && numBoxB0 == 0  ->  break");
                    break;
                }
            }
        }
    }


    private int ruleTrain(int unrollSteps, List<DurAndMem> durations) throws InterruptedException, ExecutionException {
        int epoch;
        DurAndMem duration = new DurAndMem();
        duration.on();
        boolean[] freeze = new boolean[]{false, true, true};
        modelService.trainModelRules(freeze, unrollSteps).get();

        epoch = modelState.getEpoch();
        //  trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();
        duration.off();
        durations.add(duration);
        System.out.println("epoch;duration[ms];gpuMem[MiB]");
        IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));
        return epoch;
    }

//        gameBuffer.resetRelevantIds();
//        gameBuffer.clearEpisodeIds();
//
//    }

    private long numBoxA0() {
        long firstBoxes = timestepRepo.numBoxA0();
        log.info("num in boxA=0: {}",  firstBoxes);
        return firstBoxes;
    }
    private long numBoxB0() {
        long firstBoxes = timestepRepo.numBoxB0();
        log.info("num in boxB=0: {}",  firstBoxes);
        return firstBoxes;
    }



}
