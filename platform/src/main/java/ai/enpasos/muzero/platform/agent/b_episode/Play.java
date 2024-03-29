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
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.common.FileUtils;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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
    ModelState modelState;

    @Autowired
    PlayService playService;

    public void fillingBuffer(boolean isRandomFill) {
        int windowSize = config.getWindowSize();
        while (!gameBuffer.getBuffer().isBufferFilled()) {
            log.info(gameBuffer.getBuffer().getGames().size() + " of " + windowSize);
             playMultipleEpisodes(false, isRandomFill);
        }
    }


    public void deleteNetworksAndGames() {
            FileUtils.rmDir(config.getOutputDir());
            FileUtils.mkDir( config.getNetworkBaseDir());
            FileUtils.mkDir( config.getGamesBasedir());
    }


    public void playGames(boolean render, int trainingStep) {
        log.info("last training step = {}", trainingStep);
        log.info("numSimulations: " + config.getNumSimulations());

        playMultipleEpisodes(render, false);

    }


    public void playMultipleEpisodes(boolean render, boolean fastRuleLearning) {
        List<Game> games;
        List<Game> gamesToReanalyse = null;
        if (config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            gamesToReanalyse = gameBuffer.getGamesToReanalyse();
        }
        if (config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            games = playService.reanalyseGames(config.getNumParallelGamesPlayed(),
                PlayParameters.builder()
                    .render(render)
                    .fastRulesLearning(fastRuleLearning)
                    .replay(true)
                    .build(),
                gamesToReanalyse);
        } else {
            games = playService.playNewGames(config.getNumParallelGamesPlayed(),
                PlayParameters.builder()
                    .render(render)
                    .fastRulesLearning(fastRuleLearning)
                    .build());
        }

        log.info("Played {} games parallel", games.size());

        gameBuffer.addGames(games, false);
    }

}
