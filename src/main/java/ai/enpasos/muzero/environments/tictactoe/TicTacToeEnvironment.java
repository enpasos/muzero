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

package ai.enpasos.muzero.environments.tictactoe;

import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.environments.EnvironmentBaseBoardGames;
import ai.enpasos.muzero.environments.OneOfTwoPlayer;
import ai.enpasos.muzero.play.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TicTacToeEnvironment extends EnvironmentBaseBoardGames {

    private final static Logger logger = LoggerFactory.getLogger(TicTacToeEnvironment.class);

    // values
    // 0: empty field,
    // 1: PlayerB,
    // -1: PlayerA
    //   int[][] board;
    //   int[][] boardTransfer;

    //  OneOfTwoPlayer playerToMove;


    public TicTacToeEnvironment(MuZeroConfig config) {
        super(config);
//            board = new int[config.getBoardHeight()][config.getBoardWidth()];
//            playerToMove = OneOfTwoPlayer.PlayerA;
    }


    @Override
    public float step(Action action) {

        // putting the stone for the player
        int col = action.getCol();
        int row = action.getRow();
        if (this.board[row][col] == 0) {
            this.board[row][col] = playerToMove.getValue();
        } else {
            throw new RuntimeException("illegal Move");
        }

        float reward = reward();

        swapPlayer();

        return reward;
    }

    private float reward() {
        float reward = 0f;
        if (hasPlayerWon(this.playerToMove)) {
            reward = 1f;
        } else if (hasPlayerWon(OneOfTwoPlayer.otherPlayer(this.playerToMove))) {
            reward = -1f;
        }
        return reward;
    }

    public void swapPlayer() {
        this.playerToMove = OneOfTwoPlayer.otherPlayer(this.playerToMove);
    }


    private boolean isLegalAction(Action action_) {
        return legalActions().contains(action_);
    }


    public List<Action> legalActions() {
        List<Action> legal = new ArrayList<>();
        for (int col = 0; col < config.getBoardWidth(); col++) {
            for (int row = 0; row < config.getBoardHeight(); row++) {
                if (this.board[row][col] == 0) {
                    legal.add(new Action(config, row * config.getBoardWidth() + col));
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

    public boolean hasPlayerWon(OneOfTwoPlayer player) {
        return checkIfPlayerHasWon(player, board, 3);
    }

    public String render() {
        return render(config, preRender());
    }

    private boolean checkIfPlayerHasWon(OneOfTwoPlayer player, int[][] b, int numInARow) {
        int p = player.getValue();

        // Horizontal check
        for (int x = 0; x < config.getBoardWidth() + 1 - numInARow; x++) {
            y:
            for (int y = 0; y < config.getBoardHeight(); y++) {
                for (int r = 0; r < numInARow; r++) {
                    if (b[y][x + r] != p) continue y;
                }
                return true;
            }
        }
        // Vertical check
        for (int x = 0; x < config.getBoardWidth(); x++) {
            y:
            for (int y = 0; y < config.getBoardHeight() + 1 - numInARow; y++) {
                for (int r = 0; r < numInARow; r++) {
                    if (b[y + r][x] != p) continue y;
                }
                return true;
            }
        }
        // x diag check
        for (int x = 0; x < config.getBoardWidth() + 1 - numInARow; x++) {
            y:
            for (int y = 0; y < config.getBoardHeight() + 1 - numInARow; y++) {
                for (int r = 0; r < numInARow; r++) {
                    if (b[y + r][x + r] != p) continue y;
                }
                return true;
            }
        }
        // -x diag check
        for (int x = 0; x < config.getBoardWidth() + 1 - numInARow; x++) {
            y:
            for (int y = numInARow - 1; y < config.getBoardHeight(); y++) {
                for (int r = 0; r < numInARow; r++) {
                    if (b[y - r][x + r] != p) continue y;
                }
                return true;
            }
        }
        return false;
    }


}
