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
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueRepo;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.FillRulesLoss;
import ai.enpasos.muzero.platform.run.FillValueTable;
import ai.enpasos.muzero.platform.run.TemperatureCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

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
    TemperatureCalculator temperatureCalculator;

    @Autowired
    FillRulesLoss fillRulesLoss;


    @SuppressWarnings("java:S106")
    public void train(TrainParams params) throws InterruptedException, ExecutionException {

        int trainingStep = 0;
        int epoch = 0;

        List<DurAndMem> durations = new ArrayList<>();

        modelService.loadLatestModelOrCreateIfNotExisting().get();
        epoch = modelState.getEpoch();

        gameBuffer.loadLatestStateIfExists();
        play.fillingBuffer(params.isRandomFill());


        while (trainingStep < config.getNumberOfTrainingSteps()) {


            DurAndMem duration = new DurAndMem();
            duration.on();

//            if (epoch != 0) {
//                PlayTypeKey originalPlayTypeKey = config.getPlayTypeKey();
//                for (PlayTypeKey key : config.getPlayTypeKeysForTraining()) {
//                    config.setPlayTypeKey(key);
//                    play.playGames(params.isRender(), trainingStep);
//                }
//                config.setPlayTypeKey(originalPlayTypeKey);
//            }

//            log.info("game counter: " + gameBuffer.getPlanningBuffer().getCounter());
//            log.info("window size: " + gameBuffer.getPlanningBuffer().getWindowSize());
//            log.info("gameBuffer size: " + this.gameBuffer.getPlanningBuffer().getEpisodeMemory().getGameList().size());


           // log.info("Epoch(" + epoch + ")");
           // if (epoch % 10 == 0) {
//                log.info("fillRewardLoss.fillRewardLossForNetworkOfEpoch(" + epoch + ")");
//                fillRulesLoss.evaluatedRulesLearningForNetworkOfEpoch(epoch );
         //   }

           boolean[] freeze = new boolean[]{false, true, true};
//            int unknowns = 0;
//            do {
//                for (int i = 0; i < 10; i++) {
                    modelService.trainModel(freeze, RULES_BUFFER, false).get();
//                }
             //     epoch = modelState.getEpoch();
//                fillRulesLoss.evaluatedRulesLearningForNetworkOfEpochForBox0(epoch);
//                unknowns = fillRulesLoss.numBox(0);
//                log.info("unknowns: " + unknowns);
//            } while (unknowns > 0);

//            freeze = new boolean[]{true, false, false};
//            modelService.trainModel(freeze, PLANNING_BUFFER, true).get();

            epoch = modelState.getEpoch();

            trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();

            duration.off();
            durations.add(duration);
            System.out.println("epoch;duration[ms];gpuMem[MiB]");
            IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));

        }

    }


}
