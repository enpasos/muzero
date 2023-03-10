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


import ai.enpasos.muzero.platform.agent.a_loopcontrol.play.PlayParameters;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.play.service.PlayService;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.common.FileUtils;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

@Slf4j
@Component
public class MuZero {

    @Autowired
    MuZeroConfig config;


    @Autowired
    GameBuffer gameBuffer;



    @Autowired
    ModelService modelService;


    @Autowired
    ModelState modelState;

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


    public void deleteNetworksAndGames() {

            FileUtils.rmDir(config.getOutputDir());


            FileUtils.mkDir( config.getNetworkBaseDir());
            FileUtils.mkDir( config.getGamesBasedir());

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

    public void loadBuffer(boolean freshBuffer) {
        gameBuffer.init();
        if (!freshBuffer) {
            gameBuffer.loadLatestState();
        }
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
