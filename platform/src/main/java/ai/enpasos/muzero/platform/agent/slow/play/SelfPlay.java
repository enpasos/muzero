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

package ai.enpasos.muzero.platform.agent.slow.play;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;


@Slf4j
@Component
public class SelfPlay {

    @Autowired
    MuZeroConfig config;

    @Autowired
    Episode episode;

    @Autowired
    ReplayBuffer replayBuffer;

    public @NotNull List<Game> playGame(Network network, boolean render, boolean fastRuleLearning, boolean explorationNoise) {
        episode.init();
        if (render) {
            log.debug(episode.justOneOfTheGames().render());
        }
        try (NDManager nDManager = network.getNDManager().newSubManager()) {
            List<NDArray> actionSpaceOnDevice = Network.getAllActionsOnDevice(config, nDManager);
            network.setActionSpaceOnDevice(actionSpaceOnDevice);
            network.createAndSetHiddenStateNDManager(nDManager, true);
            while (episode.notFinished()) {
                episode.play(network, render, fastRuleLearning, explorationNoise);
            }
        }

        long duration = System.currentTimeMillis() - episode.getStart();
        log.info("duration game play [ms]: {}", duration);
        log.info("inference duration game play [ms]: {}", episode.getInferenceDuration().value);
        log.info("java duration game play [ms]: {}", (duration - episode.getInferenceDuration().value));

        return episode.getGamesDoneList();
    }


    public void playMultipleEpisodes(Network network, boolean render, boolean fastRuleLearning, boolean explorationNoise) {
        IntStream.range(0, config.getNumEpisodes()).forEach(i ->
        {
            List<Game> gameList = playGame(network, render, fastRuleLearning, explorationNoise);
            gameList.forEach(replayBuffer::saveGame);

            log.info("Played {} games parallel, round {}", config.getNumParallelGamesPlayed(), i);
        });
    }
}


