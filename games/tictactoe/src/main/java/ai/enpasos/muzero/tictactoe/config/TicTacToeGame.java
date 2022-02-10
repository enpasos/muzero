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

package ai.enpasos.muzero.tictactoe.config;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.intuitive.Observation;
import ai.enpasos.muzero.platform.agent.memory.GameDTO;
import ai.enpasos.muzero.platform.agent.memory.ZeroSumGame;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.agent.rational.Node;
import ai.enpasos.muzero.platform.agent.rational.Player;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.EnvironmentBase;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@SuppressWarnings("squid:S2160")
public class TicTacToeGame extends ZeroSumGame {

    public static final String PASS = "pass: ";
    final float[] @NotNull [] boardtransfer;


    public TicTacToeGame(@NotNull MuZeroConfig config, GameDTO gameDTO) {
        super(config, gameDTO);
        initEnvironment();
        boardtransfer = new float[config.getBoardHeight()][config.getBoardWidth()];
    }

    public TicTacToeGame(@NotNull MuZeroConfig config) {
        super(config);
        environment = new TicTacToeEnvironment(config);
        boardtransfer = new float[config.getBoardHeight()][config.getBoardWidth()];
    }

    @Override
    public @NotNull TicTacToeEnvironment getEnvironment() {
        return (TicTacToeEnvironment) environment;
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
        environment = new TicTacToeEnvironment(config);
        if (stateIndex == -1) return;
        for (int i = 0; i < stateIndex; i++) {
            Action action = config.newAction(this.getGameDTO().getActions().get(i));
            environment.step(action);
        }
    }

    private float[] @NotNull [] getBoardPositions(int[] @NotNull [] board, int p) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                boardtransfer[i][j] = board[i][j] == p ? 1 : 0;
            }
        }
        return boardtransfer;
    }

    @SuppressWarnings("squid:S2095")
    public @NotNull Observation getObservation(@NotNull NDManager ndManager) {

        OneOfTwoPlayer currentPlayer = this.getEnvironment().getPlayerToMove();
        OneOfTwoPlayer opponentPlayer = OneOfTwoPlayer.otherPlayer(this.getEnvironment().getPlayerToMove());


        // values in the range [0, 1]
        NDArray boardCurrentPlayer = ndManager.create(getBoardPositions(this.getEnvironment().currentImage(), currentPlayer.getValue()));
        NDArray boardOpponentPlayer = ndManager.create(getBoardPositions(this.getEnvironment().currentImage(), opponentPlayer.getValue()));

        // workaround for
        //    NDArray boardColorToPlay = ndManager.full(new Shape(config.getBoardHeight(), config.getBoardWidth()), currentPlayer.getActionValue());
        float[][] data = new float[config.getBoardHeight()][config.getBoardWidth()];  // TODO check correct ordering
        for(int i = 0;  i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = currentPlayer.getActionValue();
            }
        }
        NDArray boardColorToPlay = ndManager.create(data );

        NDArray stacked = NDArrays.stack(new NDList(boardCurrentPlayer, boardOpponentPlayer, boardColorToPlay));

        return new Observation(stacked);
    }


    @Override
    public Player toPlay() {
        return this.getEnvironment().getPlayerToMove();
    }

    @Override
    public String render() {

        String r = this.getGameDTO().getActions().size() + ": ";
        OneOfTwoPlayer player = null;
        if (!this.getGameDTO().getActions().isEmpty()) {
            Action action = config.newAction(this.getGameDTO().getActions().get(this.getGameDTO().getActions().size() - 1));
            int colLastMove = ((TicTacToeAction) action).getCol();

            player = OneOfTwoPlayer.otherPlayer(this.getEnvironment().getPlayerToMove());
            r += player.getSymbol() + " move (" + (((TicTacToeAction) action).getRow() + 1) + ", " + (char) (colLastMove + 65) + ") index " + action.getIndex();
        }
        r += "\n";
        r += getEnvironment().render();
        if (terminal() && !this.getGameDTO().getRewards().isEmpty()) {
            if (this.getGameDTO().getRewards().get(this.getGameDTO().getRewards().size() - 1) == 0.0f) {
                r += "\ndraw";
            } else {
                r += "\nwinning: " + Objects.requireNonNull(player).getSymbol();
            }
            log.info("\nG A M E  O V E R");
        }
        return r;
    }


    public void renderMCTSSuggestion(@NotNull MuZeroConfig config, float @NotNull [] childVisits) {

        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];

        log.info("\nmcts suggestion:");
        int boardSize = config.getBoardHeight() * config.getBoardWidth();
        for (int i = 0; i < boardSize; i++) {
            values[TicTacToeAction.getRow(config, i)][TicTacToeAction.getCol(config, i)] = String.format("%2d", Math.round(100.0 * childVisits[i])) + "%";
        }
        log.info(EnvironmentBase.render(config, values));
        if (childVisits.length > boardSize) {
            log.info(PASS + String.format("%2d", Math.round(100.0 * childVisits[boardSize])) + "%");
        }
    }

    @Override
    public void initEnvironment() {
        environment = new TicTacToeEnvironment(config);
    }

    public void renderNetworkGuess(@NotNull MuZeroConfig config, @NotNull Player toPlay, @Nullable NetworkIO networkOutput, boolean gameOver) {
        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
        if (networkOutput != null) {
            double v = networkOutput.getValue();
            double p = (v + 1) / 2 * 100;
            int percent = (int) Math.round(p);
            log.info("\nnetwork guess:");
            if (!gameOver) {
                int boardSize = config.getBoardHeight() * config.getBoardWidth();
                for (int i = 0; i < boardSize; i++) {
                    values[TicTacToeAction.getRow(config, i)][TicTacToeAction.getCol(config, i)] = String.format("%2d", Math.round(100.0 * networkOutput.getPolicyValues()[i])) + "%";  // because softmax
                }
                log.info(EnvironmentBase.render(config, values));
                if (networkOutput.getPolicyValues().length > boardSize) {
                    log.info(PASS + String.format("%2d", Math.round(100.0 * networkOutput.getPolicyValues()[boardSize])) + "%");
                }

            }
            if (toPlay instanceof OneOfTwoPlayer)
                log.info("Estimated chance for " + ((OneOfTwoPlayer) toPlay).getSymbol() + " to win: " + percent + "%");

        }
    }

    public void renderSuggestionFromPriors(@NotNull MuZeroConfig config, @NotNull Node node) {
        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];

        log.info("\nwith exploration noise suggestion:");
        int boardSize = config.getBoardHeight() * config.getBoardWidth();
        for (int i = 0; i < boardSize; i++) {
            Action a = config.newAction(i);
            float value = 0f;
            if (node.getChildren().containsKey(a)) {
                value = (float) node.getChildren().get(a).getPrior();
            }
            values[TicTacToeAction.getRow(config, i)][TicTacToeAction.getCol(config, i)]
                    = String.format("%2d", Math.round(100.0 * value)) + "%";
        }

        log.info(EnvironmentBase.render(config, values));
        if (boardSize < config.getActionSpaceSize()) {
            Action a = config.newAction(boardSize);
            float value = 0f;
            if (node.getChildren().containsKey(a)) {
                value = (float) node.getChildren().get(a).getPrior();
            }
            log.info(PASS + String.format("%2d", Math.round(100.0 * value)) + "%");
        }
    }


}
