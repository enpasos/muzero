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

package ai.enpasos.muzero.platform.agent.b_episode;


import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.common.FileUtils;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
public class Play {

    @Autowired
    MuZeroConfig config;


    @Autowired
    GameBuffer gameBuffer;



    @Autowired
    ModelService modelService;



    @Autowired
    EpisodeRepo episodeRepo;


    @Autowired
    ModelState modelState;

    @Autowired
    PlayService playService;

    @Autowired
    Inference inference;

    public void fillingBuffer(boolean isRandomFill) {
        int windowSize = config.getWindowSize();
        while (!gameBuffer.getBuffer().isBufferFilled()) {
            log.info(gameBuffer.getBuffer().getGames().size() + " of " + windowSize);
             playMultipleEpisodes(false, isRandomFill, 0);
        }
    }


    public void deleteNetworksAndGames() {
            FileUtils.rmDir(config.getOutputDir());
            FileUtils.mkDir( config.getNetworkBaseDir());
            FileUtils.mkDir( config.getGamesBasedir());
    }


    public void playGames(boolean render, int trainingStep, int epoch) {
        log.info("last training step = {}", trainingStep);
        log.info("numSimulations: " + config.getNumSimulations());

        playMultipleEpisodes(render, false, epoch);

    }


    public void playMultipleEpisodes(boolean render, boolean fastRuleLearning, int epoch) {
        List<Game> games;
        if (config.getPlayTypeKey() == PlayTypeKey.REWARD_MEMORIZATION_CHECK) {
            if (epoch % 10 == 0) {
                checkRewardMemorization();
            }

             return;
        } else if (config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            List<Game> gamesToReanalyse = gameBuffer.getGamesToReanalyse();
            if (config.getReplayTimestepsFromEnd() > 0) {
                games = playService.reanalyseGames(config.getNumParallelGamesPlayed(),
                        PlayParameters.builder()
                                .render(render)
                                .fastRulesLearning(fastRuleLearning)
                                .replay(true)
                                .build(),
                        gamesToReanalyse);
            } else {
                games = gamesToReanalyse;
            }
        } else if (config.getPlayTypeKey() == PlayTypeKey.HYBRID2) {
            List<Game> gamesToPlay = gameBuffer.getGamesWithHighestTemperatureTimesteps();
            games = playService.hybrid2Games(config.getNumParallelGamesPlayed(),
                    PlayParameters.builder()
                            .render(render)
                            .fastRulesLearning(fastRuleLearning)
                            .hybrid2(true)
                            .build(),
                    gamesToPlay);
            // now it is hybrid
            games.forEach(g ->  g.getEpisodeDO().setHybrid(true));
        } else {
            games = playService.playNewGames(config.getNumParallelGamesPlayed(),
                PlayParameters.builder()
                    .render(render)
                    .epoch(epoch)
                    .numModels(10)
                    .fastRulesLearning(fastRuleLearning)
                    .build());
        }

        log.info("Played {} games parallel", games.size());

        gameBuffer.addGames(games, false);
    }

    private void checkRewardMemorization() {
        List<Game> games;
        long maxCount = episodeRepo.getMaxCount();
        int increase = 10000;
        for (int c = 0; c <= maxCount; c += increase) {
            games = gameBuffer.getGamesToCheckRewardMemorization(c, c + increase - 1);
            double[] values = inference.aiValue(games, false);
            for (int i = 0; i < values.length; i++) {
                episodeRepo.setRewardExpectationFromModel(games.get(i).getEpisodeDO().getId(), (float) values[i]);
            }
        }
    }

}
