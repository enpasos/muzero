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

package ai.enpasos.muzero.platform.agent.rational;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


@Slf4j
@Component
public class SelfPlay {

    @Autowired
    MuZeroConfig config;

    @Autowired
    EpisodeManager episode;

    @Autowired
    ReplayBuffer replayBuffer;

    public @NotNull List<Game> playGame(Network network, boolean render, boolean fastRuleLearning, boolean explorationNoise) {
        episode.init();
        if (render) {
            log.debug(episode.justOneOfTheGames().render());
        }
        runEpisode(network, render, fastRuleLearning);
        long duration = System.currentTimeMillis() - episode.getStart();
        log.info("duration game play [ms]: {}", duration);
        log.info("inference duration game play [ms]: {}", episode.getInferenceDuration().value);
        log.info("java duration game play [ms]: {}", (duration - episode.getInferenceDuration().value));
        return episode.getGamesDoneList();
    }

    public @NotNull Game playGameFromCurrentState(Network network, Game replayGame, boolean untilEnd) {
        log.info("playGameFromCurrentState");
        episode.init(List.of(replayGame));
        runEpisode(network, false, false, untilEnd);
        long duration = System.currentTimeMillis() - episode.getStart();
        log.info("duration replay [ms]: {}", duration);
        log.info("inference duration replay [ms]: {}", episode.getInferenceDuration().value);
        log.info("java duration replay [ms]: {}", (duration - episode.getInferenceDuration().value));
        return episode.getGameList().get(0);
    }

    public void playOneActionFromCurrentState(Network network, Game replayGame) {
        episode.init(List.of(replayGame));
        episode.play(network, false, false);
    }

    public @NotNull List<Game> playGamesFromTheirCurrentState(Network network, List<Game> replayGames) {
        log.info("playGamesFromTheirCurrentState");
        episode.init(replayGames);
        runEpisode(network, false, false);
        long duration = System.currentTimeMillis() - episode.getStart();
        log.info("duration replay [ms]: {}", duration);
        log.info("inference duration replay [ms]: {}", episode.getInferenceDuration().value);
        log.info("java duration replay [ms]: {}", (duration - episode.getInferenceDuration().value));
        return episode.getGamesDoneList();
    }

    private void runEpisode(Network network, boolean render, boolean fastRulesLearning) {
        runEpisode(network, render, fastRulesLearning, true);
    }

    private void runEpisode(Network network, boolean render, boolean fastRulesLearning, boolean untilEnd) {
        try (NDManager nDManager = network.getNDManager().newSubManager()) {
            List<NDArray> actionSpaceOnDevice = Network.getAllActionsOnDevice(config, nDManager);
            network.setActionSpaceOnDevice(actionSpaceOnDevice);
            network.createAndSetHiddenStateNDManager(nDManager, true);
            int count = 1;
            while (episode.notFinished() && (untilEnd || count == 1)) {
                episode.play(network, render, fastRulesLearning);
                log.info("episode run " + count + " for " + config.getNumParallelGamesPlayed() + " games finished.");
                count++;
            }
        }
    }

    public @NotNull List<Game> justReplayGamesWithInitialInference(Network network, List<Game> inputGames) {
        episode.init(inputGames);

        try (NDManager nDManager = network.getNDManager().newSubManager()) {
            List<NDArray> actionSpaceOnDevice = Network.getAllActionsOnDevice(config, nDManager);
            network.setActionSpaceOnDevice(actionSpaceOnDevice);
            network.createAndSetHiddenStateNDManager(nDManager, true);
            while (episode.notFinished()) {
                episode.justReplayWithInitialInference(network);
            }
        }

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


    public void replayGamesToEliminateSurprise(Network network, List<Game> gamesToReplay) {
        log.info("replayGamesToEliminateSurprise, {} games", gamesToReplay.size());
        List<List<Game>> gameBatches = ListUtils.partition(gamesToReplay, config.getNumParallelGamesPlayed());

        List<Game> resultGames = new ArrayList<>();
        for (List<Game> gameList : gameBatches) {
            resultGames.addAll(playGamesFromTheirCurrentState(network, gameList));
        }
        resultGames.forEach(replayBuffer::saveGame);
    }
}


