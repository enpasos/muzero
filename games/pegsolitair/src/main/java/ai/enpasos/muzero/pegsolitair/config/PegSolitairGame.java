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

package ai.enpasos.muzero.pegsolitair.config;

import ai.enpasos.muzero.pegsolitair.config.environment.Board;
import ai.enpasos.muzero.pegsolitair.config.environment.Point;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.Observation;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameDTO;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.play.Action;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.play.Player;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.pegsolitair.config.environment.NeighborMap.inRange;

public class PegSolitairGame extends Game {


    public PegSolitairGame(@NotNull MuZeroConfig config, GameDTO gameDTO) {
        super(config, gameDTO);
        initEnvironment();
    }

    public PegSolitairGame(@NotNull MuZeroConfig config) {
        super(config);
        initEnvironment();
    }

    @Override
    public @NotNull PegSolitairEnvironment getEnvironment() {
        return (PegSolitairEnvironment) environment;
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
        return IntStream.range(0, config.getActionSpaceSize()).mapToObj(i -> config.newAction(i)).collect(Collectors.toList());
    }


    public void replayToPosition(int stateIndex) {
        environment = new PegSolitairEnvironment(config);
        if (stateIndex == -1) return;
        for (int i = 0; i < stateIndex; i++) {
            Action action = config.newAction(this.getGameDTO().getActions().get(i));
            environment.step(action);
        }
    }


    public @NotNull Observation getObservation() {

        Board board = ((PegSolitairEnvironment) environment).getBoard();

        return new Observation(getBoardPositions(board), new long[]{1L, 7L, 7L});
    }


    private float[] getBoardPositions(Board board) {
        int size = 7;
        float[] boardtransfer = new float[size * size];
        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                Point p = new Point(row, col);
                if (board.getPegsOnTheBoard().contains(p)) {
                    boardtransfer[(row - 1) * size + col - 1] = 1f;
                } else if (board.getHolesOnTheBoard().contains(p)) {
                    boardtransfer[(row - 1) * size + col - 1] = 0f;
                } else if (!inRange(p)) {
                    boardtransfer[(row - 1) * size + col - 1] = 0f;
                }
            }
        }
        return boardtransfer;
    }


    @Override
    public Player toPlay() {
        return null;
    }

    @Override
    public String render() {
        return ((PegSolitairEnvironment) environment).render();
    }


    public void renderMCTSSuggestion(@NotNull MuZeroConfig config, float @NotNull [] childVisits) {
        // not implemented, yet
    }

    @Override
    public void initEnvironment() {
        environment = new PegSolitairEnvironment(config);
    }

    public void renderNetworkGuess(@NotNull MuZeroConfig config, Player toPlay, @Nullable NetworkIO networkOutput, boolean gameOver) {
        // not implemented, yet
    }

    public void renderSuggestionFromPriors(@NotNull MuZeroConfig config, @NotNull Node node) {
        // not implemented, yet
    }
}
