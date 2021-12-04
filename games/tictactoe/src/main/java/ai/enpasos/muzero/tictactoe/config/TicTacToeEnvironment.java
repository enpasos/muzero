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

import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.EnvironmentZeroSumBase;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TicTacToeEnvironment extends EnvironmentZeroSumBase {

    private final static Logger logger = LoggerFactory.getLogger(TicTacToeEnvironment.class);


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
            throw new RuntimeException("illegal Move");
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

    public void swapPlayer() {
        this.setPlayerToMove(OneOfTwoPlayer.otherPlayer(this.getPlayerToMove()));
    }


    private boolean isLegalAction(Action action_) {
        return legalActions().contains(action_);
    }


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

    public boolean terminal() {
        return hasPlayerWon(OneOfTwoPlayer.PlayerA) || hasPlayerWon(OneOfTwoPlayer.PlayerB);
    }

    public boolean hasPlayerWon(@NotNull OneOfTwoPlayer player) {
        return checkIfPlayerHasWon(player, board);
    }

    public @NotNull String render() {
        return render(config, preRender());
    }

    private boolean checkIfPlayerHasWon(@NotNull OneOfTwoPlayer player, int[][] b) {
        int p = player.getValue();

        // Horizontal check
        for (int x = 0; x < config.getBoardWidth() + 1 - 3; x++) {
            y:
            for (int y = 0; y < config.getBoardHeight(); y++) {
                for (int r = 0; r < 3; r++) {
                    if (b[y][x + r] != p) continue y;
                }
                return true;
            }
        }
        // Vertical check
        for (int x = 0; x < config.getBoardWidth(); x++) {
            y:
            for (int y = 0; y < config.getBoardHeight() + 1 - 3; y++) {
                for (int r = 0; r < 3; r++) {
                    if (b[y + r][x] != p) continue y;
                }
                return true;
            }
        }
        // x diag check
        for (int x = 0; x < config.getBoardWidth() + 1 - 3; x++) {
            y:
            for (int y = 0; y < config.getBoardHeight() + 1 - 3; y++) {
                for (int r = 0; r < 3; r++) {
                    if (b[y + r][x + r] != p) continue y;
                }
                return true;
            }
        }
        // -x diag check
        for (int x = 0; x < config.getBoardWidth() + 1 - 3; x++) {
            y:
            for (int y = 3 - 1; y < config.getBoardHeight(); y++) {
                for (int r = 0; r < 3; r++) {
                    if (b[y - r][x + r] != p) continue y;
                }
                return true;
            }
        }
        return false;
    }


}
