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

import ai.enpasos.muzero.platform.agent.b_planning.Action;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.EnvironmentZeroSumBase;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TicTacToeEnvironment extends EnvironmentZeroSumBase {


    public TicTacToeEnvironment(@NotNull MuZeroConfig config) {
        super(config);
    }


    @Override
    public float step(@NotNull Action action) {

        // putting the stone for the player
        int col = ((TicTacToeAction) action).getCol();
        int row = ((TicTacToeAction) action).getRow();
        if (this.board[row][col] == 0) {
            this.board[row][col] = getPlayerToMove().getValue();
        } else {
            throw new MuZeroException("illegal Move");
        }

        float reward = reward();

        swapPlayer();

        return reward;
    }

    private float reward() {
        float reward = 0f;
        if (hasPlayerWon(this.getPlayerToMove())) {
            reward = 1f;
        } else if (hasPlayerWon(OneOfTwoPlayer.otherPlayer(this.getPlayerToMove()))) {
            reward = -1f;
        }
        return reward;
    }

    @Override
    public void swapPlayer() {
        this.setPlayerToMove(OneOfTwoPlayer.otherPlayer(this.getPlayerToMove()));
    }

    @Override
    public @NotNull List<Action> legalActions() {
        List<Action> legal = new ArrayList<>();
        for (int col = 0; col < config.getBoardWidth(); col++) {
            for (int row = 0; row < config.getBoardHeight(); row++) {
                if (this.board[row][col] == 0) {
                    legal.add(config.newAction(row * config.getBoardWidth() + col));
                }
            }
        }
        return legal;
    }

    @Override
    public int[][] currentImage() {
        return this.board;
    }

    @Override
    public boolean terminal() {
        return hasPlayerWon(OneOfTwoPlayer.PLAYER_A) || hasPlayerWon(OneOfTwoPlayer.PLAYER_B);
    }

    @Override
    public boolean hasPlayerWon(@NotNull OneOfTwoPlayer player) {
        return checkIfPlayerHasWon(player, board);
    }

    public @NotNull String render() {
        return render(config, preRender());
    }

    public boolean checkIfPlayerHasWon(@NotNull OneOfTwoPlayer player, int[][] b) {
        int p = player.getValue();
        return horizontalCheck(b, p)
            || verticalCheck(b, p)
            || diagonalCheck(b, p)
            || reverseDiagonalCheck(b, p);
    }

    private boolean foundThreeStonesOnALine(int[][] b, int p, int y, int x, int signY, int signX) {
        for (int r = 0; r < 3; r++) {
            if (b[y + signY * r][x + signX * r] != p) return false;
        }
        return true;
    }

    private boolean reverseDiagonalCheck(int[][] b, int p) {
        for (int x = 0; x < config.getBoardWidth() + 1 - 3; x++) {
            for (int y = 3 - 1; y < config.getBoardHeight(); y++) {
                if (foundThreeStonesOnALine(b, p, y, x, -1, 1)) return true;
            }
        }
        return false;
    }

    private boolean diagonalCheck(int[][] b, int p) {
        for (int x = 0; x < config.getBoardWidth() + 1 - 3; x++) {
            for (int y = 0; y < config.getBoardHeight() + 1 - 3; y++) {
                if (foundThreeStonesOnALine(b, p, y, x, 1, 1)) return true;
            }
        }
        return false;
    }

    private boolean verticalCheck(int[][] b, int p) {
        for (int x = 0; x < config.getBoardWidth(); x++) {
            for (int y = 0; y < config.getBoardHeight() + 1 - 3; y++) {
                if (foundThreeStonesOnALine(b, p, y, x, 1, 0)) return true;
            }
        }
        return false;
    }

    private boolean horizontalCheck(int[][] b, int p) {
        for (int x = 0; x < config.getBoardWidth() + 1 - 3; x++) {
            for (int y = 0; y < config.getBoardHeight(); y++) {
                if (foundThreeStonesOnALine(b, p, y, x, 0, 1)) return true;
            }
        }
        return false;
    }


}
