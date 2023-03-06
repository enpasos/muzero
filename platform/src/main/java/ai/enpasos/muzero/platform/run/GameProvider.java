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

package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.b_planning.service.PlayService;
import ai.enpasos.muzero.platform.agent.c_model.Network;
import ai.enpasos.muzero.platform.agent.c_model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.c_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.d_experience.Game;
import ai.enpasos.muzero.platform.agent.d_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.b_planning.SelfPlay;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Map.entry;


@Slf4j
@Component
public class GameProvider {

    @Autowired
    MuZeroConfig config;

    @Autowired
    GameBuffer gameBuffer;


    @Autowired
    PlayService playService;

    @Autowired
    ModelService modelService;


    @NotNull
    public Optional<Game> getGame() {
        gameBuffer.loadLatestState();
        return Optional.of(gameBuffer.getBuffer().getGames().get(gameBuffer.getBuffer().getGames().size() - 1));

    }

    @NotNull
    public Optional<Game> getGame(int no) {
        gameBuffer.loadLatestState();
        return Optional.of(gameBuffer.getBuffer().getGames().get(no));
    }

    public Optional<Game> getGameStartingWithActions(int... actions) {
        List<Integer> actionsList = Arrays.stream(actions).boxed().collect(Collectors.toList());
        return getGameStartingWithActions(actionsList);
    }

    public Optional<Game> getGameStartingWithActions(List<Integer> actionsList) {
        gameBuffer.loadLatestState();
        List<Game> games = gameBuffer.getBuffer().getGames();
        return games.stream().filter(game ->
            // check if game.getGameDTO().getActions() starts with actionsList
            game.getGameDTO().getActions().stream().limit(actionsList.size()).collect(Collectors.toList()).equals(actionsList)
        ).findFirst();
    }

    public Optional<Game> getGameStartingWithActionsFromStart(int... actions) {
        return getGameStartingWithActionsFromStartForEpoch(-1, actions);
    }

    public Optional<Game> getGameStartingWithActionsFromStartForEpoch(int epoch, int... actions) {

        Game game = this.config.newGame();
        game.apply(actions);


        modelService.loadLatestModel(epoch).join();

            game.setEpoch(epoch);
            measureValueAndSurprise(List.of(game));

        return Optional.of(game);

    }


    public void measureValueAndSurprise(List<Game> games) {
        games.forEach(Game::beforeReplayWithoutChangingActionHistory);
        measureValueAndSurpriseMain(games);
        games.forEach(Game::afterReplay);
    }

    private void measureValueAndSurpriseMain(List<Game> games) {
        List<List<Game>> gameBatches = ListUtils.partition(games, config.getNumParallelGamesPlayed());
        List<Game> resultGames = new ArrayList<>();
        int i = 1;
        for (List<Game> gameList : gameBatches) {
            log.debug("justReplayGamesWithInitialInference " + i++ + " of " + gameBatches.size());
            resultGames.addAll(playService.justReplayGamesWithInitialInference(gameList));
        }

    }

}
