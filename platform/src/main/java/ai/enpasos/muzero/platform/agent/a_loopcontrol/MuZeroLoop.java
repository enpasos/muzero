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
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import ai.enpasos.muzero.platform.run.FillValueTable;
import ai.enpasos.muzero.platform.run.TemperatureCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

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

            int n = 10;  // TODO: make configurable

            if (epoch != 0) {


                int lastTrainedEpoch = valueRepo.getMaxEpoch();
                log.info("identifying hot spots ...");
                temperatureCalculator.setValueHatSquaredMeanForEpochWithSummationOverLastNEpochs(lastTrainedEpoch, n);
                temperatureCalculator.aggregatePerEpisode(lastTrainedEpoch, n);
                // up to this point: valuestats is filled
                temperatureCalculator.markArchived();
                // up to this point: valuestats and episode entries are marked as archived
                // if they are not in the hot 10000


                log.info("collecting experience ...");
                PlayTypeKey originalPlayTypeKey = config.getPlayTypeKey();
                for (PlayTypeKey key : config.getPlayTypeKeysForTraining()) {
                    config.setPlayTypeKey(key);
                    play.playGames(params.isRender(), trainingStep);
                }
                config.setPlayTypeKey(originalPlayTypeKey);
            }

            log.info("reflecting on experience ...");
            fillValueTable.fillValueTableForNetworkOfEpoch(epoch);


            log.info("game counter: " + gameBuffer.getBuffer().getCounter());
            log.info("window size: " + gameBuffer.getBuffer().getWindowSize());
            log.info("gameBuffer size: " + this.gameBuffer.getBuffer().getGames().size());

            log.info("training ...");
            modelService.trainModel().get();

            epoch = modelState.getEpoch();

            trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();

            duration.off();
            durations.add(duration);
            System.out.println("epoch;duration[ms];gpuMem[MiB]");
            IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));

        }

    }


}
