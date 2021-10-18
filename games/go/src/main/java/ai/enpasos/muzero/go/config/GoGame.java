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

package ai.enpasos.muzero.go.config;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import ai.enpasos.muzero.go.config.environment.GameState;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.gamebuffer.GameDTO;
import ai.enpasos.muzero.platform.agent.fast.model.Observation;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.agent.slow.play.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GoGame extends Game {



    public GoGame(@NotNull MuZeroConfig config, GameDTO gameDTO) {
        super(config, gameDTO);
        environment = new GoEnvironment(config);
    }

    public GoGame(@NotNull MuZeroConfig config) {
        super(config);
        environment = new GoEnvironment(config);
    }

    public @NotNull GoEnvironment getEnvironment() {
        return (GoEnvironment) environment;
    }

    @Override
    public boolean terminal() {
        return this.getEnvironment().terminal();
    }


    @Override
    public List<Action> legalActions() {
        return this.getEnvironment().legalActions();
    }

    @Override
    public List<Action> allActionsInActionSpace() {
        return IntStream.range(0, config.getActionSpaceSize()).mapToObj(i -> new Action(config, i)).collect(Collectors.toList());
    }


    public void replayToPosition(int stateIndex) {
        environment = new GoEnvironment(config);
        if (stateIndex == -1) return;
        for (int i = 0; i < stateIndex; i++) {
            Action action = new Action(config, this.getGameDTO().getActionHistory().get(i));
            environment.step(action);
        }
    }




    public @NotNull Observation getObservation(@NotNull NDManager ndManager) {
        OneOfTwoPlayer currentPlayer = this.getEnvironment().getPlayerToMove();
        OneOfTwoPlayer opponentPlayer = OneOfTwoPlayer.otherPlayer(this.getEnvironment().getPlayerToMove());



        List<GameState> history = this.getEnvironment().getHistory();
        List<Optional<GameState>> relevantHistory = new ArrayList<>();
        List<NDArray> ndArrayList = new ArrayList<>();

        for (int i = 7; i >= 0; i--) {
            Optional<GameState> state = Optional.empty();
            if (history.size() > i) {
                state = Optional.of(history.get(history.size() - 1 - i));
            }
            relevantHistory.add(state);
        }


        for(Optional<GameState> optionalGameState : relevantHistory) {
            if (optionalGameState.isEmpty()) {
                ndArrayList.add(ndManager.full(new Shape(config.getBoardHeight(), config.getBoardWidth()), 0f));
                ndArrayList.add(ndManager.full(new Shape(config.getBoardHeight(), config.getBoardWidth()), 0f));
            } else {
                GameState gameState = optionalGameState.get();
                ndArrayList.addAll(GoAdapter.translate(config, ndManager, gameState));
            }
        }

        NDArray boardColorToPlay = ndManager.full(new Shape(config.getBoardHeight(), config.getBoardWidth()), currentPlayer.getActionValue());
        ndArrayList.add(boardColorToPlay);


        NDArray stacked = NDArrays.stack(new NDList(ndArrayList));

        return new Observation(stacked);
    }


    @Override
    public Player toPlay() {
        return this.getEnvironment().getPlayerToMove();
    }

    @Override
    public String render() {
        return ((GoEnvironment)environment).render();
    }
}
