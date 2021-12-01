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

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.pegsolitair.config.environment.Board;
import ai.enpasos.muzero.pegsolitair.config.environment.Point;
import ai.enpasos.muzero.platform.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.platform.agent.slow.play.Node;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.fast.model.Observation;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.gamebuffer.GameDTO;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.agent.slow.play.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.pegsolitair.config.environment.NeighborMap.inRange;

public class PegSolitairGame extends Game {


    public PegSolitairGame(@NotNull MuZeroConfig config, GameDTO gameDTO) {
        super(config, gameDTO);
        environment = new PegSolitairEnvironment(config);
    }

    public PegSolitairGame(@NotNull MuZeroConfig config) {
        super(config);
        environment = new PegSolitairEnvironment(config);
    }

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
            Action action = config.newAction(this.getGameDTO().getActionHistory().get(i));
            environment.step(action);
        }
    }



    public @NotNull Observation getObservation(@NotNull NDManager ndManager) {

        Board  board = ((PegSolitairEnvironment)environment).getBoard();

        // values in the range [0, 1]
        NDArray boardObservation = ndManager.create(getBoardPositions(board));

        NDArray stacked = NDArrays.stack(new NDList(boardObservation));

        return new Observation(stacked);
    }



    private float[] [] getBoardPositions(Board board) {
        int size = 7;
        float[][] boardtransfer = new float[size][size];
        for(int row = 1; row <= size; row++) {
            for(int col = 1; col <= size; col++) {
                Point p = new Point(row, col);
                if (board.getPegsOnTheBoard().contains(p)) {
                    boardtransfer[row-1][col-1] = 1f;
                } else if (board.getHolesOnTheBoard().contains(p)) {
                    boardtransfer[row-1][col-1] = 0f;
                } else if (!inRange(p)) {
                    boardtransfer[row-1][col-1] = 0f;
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
        return ((PegSolitairEnvironment)environment).render();
    }


    public void renderMCTSSuggestion(@NotNull MuZeroConfig config, float @NotNull [] childVisits) {

//        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
//        System.out.println();
//        System.out.println("mcts suggestion:");
//        int boardSize = config.getBoardHeight() * config.getBoardWidth();
//        for (int i = 0; i < boardSize; i++) {
//            values[PegSolitairAction.getRow(config, i)][PegSolitairAction.getCol(config, i)] = String.format("%2d", Math.round(100.0 * childVisits[i])) + "%";
//        }
//        System.out.println(EnvironmentBase.render(config, values));
//        if (childVisits.length > boardSize) {
//            System.out.println("pass: " + String.format("%2d", Math.round(100.0 * childVisits[boardSize])) + "%");
//        }
    }

    public void renderNetworkGuess(@NotNull MuZeroConfig config,  Player toPlay, @Nullable NetworkIO networkOutput, boolean gameOver) {
//        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
//        if (networkOutput != null) {
//            double v = networkOutput.getValue();
//            double p = (v + 1) / 2 * 100;
//            int percent = (int) Math.round(p);
//            System.out.println();
//            System.out.println("network guess:");
//            if (!gameOver) {
//                int boardSize = config.getBoardHeight() * config.getBoardWidth();
//                for (int i = 0; i < boardSize; i++) {
//                    values[PegSolitairAction.getRow(config, i)][PegSolitairAction.getCol(config, i)] = String.format("%2d", Math.round(100.0 * networkOutput.getPolicyValues()[i])) + "%";  // because softmax
//                }
//                System.out.println(EnvironmentBase.render(config, values));
//                if (networkOutput.getPolicyValues().length > boardSize) {
//                    System.out.println("pass: " + String.format("%2d", Math.round(100.0 * networkOutput.getPolicyValues()[boardSize])) + "%");
//                }
//
//            }
//            if (toPlay instanceof OneOfTwoPlayer)
//                System.out.println("Estimated chance for " + ((OneOfTwoPlayer) toPlay).getSymbol() + " to win: " + percent + "%");

//        }
    }

    public void renderSuggestionFromPriors(@NotNull MuZeroConfig config, @NotNull Node node) {
//        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
//        System.out.println();
//        System.out.println("with exploration noise suggestion:");
//        int boardSize = config.getBoardHeight() * config.getBoardWidth();
//        for (int i = 0; i < boardSize; i++) {
//            Action a = config.newAction(i);
//            float value = 0f;
//            if (node.getChildren().containsKey(a)) {
//                value = (float) node.getChildren().get(a).getPrior();
//            }
//            values[PegSolitairAction.getRow(config, i)][PegSolitairAction.getCol(config, i)]
//                    = String.format("%2d", Math.round(100.0 * value)) + "%";
//        }
//
//        System.out.println(EnvironmentBase.render(config, values));
//        if (boardSize < config.getActionSpaceSize()) {
//            Action a = config.newAction(boardSize);
//            float value = 0f;
//            if (node.getChildren().containsKey(a)) {
//                value = (float) node.getChildren().get(a).getPrior();
//            }
//            System.out.println("pass: " + String.format("%2d", Math.round(100.0 * value)) + "%");
//        }
    }
}
