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
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueRepo;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import ai.enpasos.muzero.platform.run.FillRulesLoss;
import ai.enpasos.muzero.platform.run.FillValueTable;
import ai.enpasos.muzero.platform.run.TemperatureCalculator;
import ai.enpasos.muzero.platform.run.TestUnrollRulestate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.config.TrainingDatasetType.PLANNING_BUFFER;
import static ai.enpasos.muzero.platform.config.TrainingDatasetType.RULES_BUFFER;

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
    ValueRepo valueRepo;


    @Autowired
    FillValueTable fillValueTable;

    @Autowired
    EpisodeRepo episodeRepo;

    @Autowired
    TimestepRepo timestepRepo;


    @Autowired
    TemperatureCalculator temperatureCalculator;

    @Autowired
    FillRulesLoss fillRulesLoss;

    @Autowired
    TestUnrollRulestate testUnrollRulestate;

    @Autowired
    DBService dbService;


    @SuppressWarnings("java:S106")
    public void train(TrainParams params) throws InterruptedException, ExecutionException {

        int trainingStep = 0;
        int epoch = 0;

        boolean currentTest = false;

        List<DurAndMem> durations = new ArrayList<>();

        modelService.loadLatestModelOrCreateIfNotExisting().get();
        epoch = modelState.getEpoch();

        gameBuffer.loadLatestStateIfExists();



        if (episodeRepo.count() < config.getInitialRandomEpisodes()) {
            play.randomEpisodes(config.getInitialRandomEpisodes()-(int)episodeRepo.count());
        }

    //    timestepRepo.resetBoxAndSAndUOk();

        boolean policyValueTraining = false;   // true: policy and value training, false: rules training
        boolean rulesTraining = true;



        TestUnrollRulestate.Result r = null;
     //   int unrollSteps = 1;
     //   long numBox0 = timestepRepo.numBox(0);

//        boolean testInitially = false;
//        if (testInitially) {
         int    unrollSteps = config.getMaxUnrollSteps();
//            log.info("testUnrollRulestate.run({})", unrollSteps);
             r = testUnrollRulestate.run(unrollSteps);
//            currentTest = true;
//            if (unrollSteps != r.getUnrollSteps()) {
//                log.info("unrollSteps != r.getUnrollSteps(); unrollSteps: {}; r.getUnrollSteps(): {}", unrollSteps, r.getUnrollSteps());
//                unrollSteps = r.getUnrollSteps();
//                log.info("testUnrollRulestate.run({})", unrollSteps);
//                r = testUnrollRulestate.run(unrollSteps);
//            }
        long numBox0 = r.getBox0();
//        }


        unrollSteps = getUnrollSteps();


        log.info("numBox0: " + numBox0);
        boolean tested =  true;


        while (unrollSteps <= config.getMaxUnrollSteps() && trainingStep < config.getNumberOfTrainingSteps()) {

            log.info("numBox0: " + numBox0);
            while (numBox0 > 0) {
                tested = false;

                testUnrollRulestate.identifyRelevantTimestepsAndTestThem(unrollSteps);
                unrollSteps = getUnrollSteps();

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


                boolean[] freeze = new boolean[]{false, true, true};
                if (rulesTraining) {
                    modelService.trainModelRules(freeze, unrollSteps).get();
                }

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

                numBox0 = timestepRepo.numBox(0);
                log.info("nBox: " + numBox0);
                currentTest = false;
            }


//            if (numBox0 == 0 && !currentTest) {
//                log.info("numBox0 == 0; testUnrollRulestate.run({})", unrollSteps);
//                r = testUnrollRulestate.run(unrollSteps  );
//                currentTest = true;
//                numBox0 = r.getBox0();
//            }
            while (numBox0 == 0 && unrollSteps < config.getMaxUnrollSteps()) {
                log.info("numBox0 == 0; unrollSteps: {}; maxUnrollSteps: {}", unrollSteps, config.getMaxUnrollSteps());
                unrollSteps++;
                currentTest = false;
                log.info("testUnrollRulestate.run({})", unrollSteps);
                r = testUnrollRulestate.run(unrollSteps  );
                currentTest = true;
                numBox0 = r.getBox0();
            }

            log.info("numBox0: " + numBox0);

            if (unrollSteps == config.getMaxUnrollSteps() && numBox0 == 0) {
                log.info("numBox0 == 0; unrollSteps == maxUnrollSteps: {}", config.getMaxUnrollSteps());
                if (!currentTest) {
                    r = testUnrollRulestate.run(unrollSteps );
                }
                numBox0 = r.getBox0();
                if (numBox0 == 0) {
                    log.info("numBox0 == 0; break;");
                    break;
                }
            }
        }


       policyValueTraining = true;   // true: policy and value training, false: rules training
       rulesTraining = false;


        while ( trainingStep < config.getNumberOfTrainingSteps()) {



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


        log.info("done" );
    }

    private int getUnrollSteps() {
        int unrollSteps;
        unrollSteps = timestepRepo.minUokNotClosed() + 1;
        unrollSteps = Math.max(1, unrollSteps);
        return unrollSteps;
    }


}
