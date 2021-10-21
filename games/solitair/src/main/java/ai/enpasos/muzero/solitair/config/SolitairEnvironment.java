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

package ai.enpasos.muzero.solitair.config;

import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.environment.Environment;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Data;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
@Data
public class SolitairEnvironment implements Environment, Serializable {

    private final static Logger logger = LoggerFactory.getLogger(SolitairEnvironment.class);


    public SolitairEnvironment(@NotNull MuZeroConfig config) {
        this.config = config;
        // -1 = not allowed position
        // 1 = filled position
        // 0 = empty position
        board = new int[][] {
                {-1, -1, 1, 1, 1, -1, -1},
                {-1, -1, 1, 1, 1, -1, -1},
                {1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 0, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1},
                {-1, -1, 1, 1, 1, -1, -1},
                {-1, -1, 1, 1, 1, -1, -1}
        };
        // jumps from right: total 19 -> 4*19 = 76 partial moves
//         board = new int[][] {
//                {-1, -1, 1, 1, X, -1, -1},
//                {-1, -1, 1, 1, X, -1, -1},
//                {1, 1, X, X, X, X, X},
//                {1, 1, X, X, X, X, X},
//                {1, 1, X, X, X, X, X},
//                {-1, -1, 1, 1, X, -1, -1},
//                {-1, -1, 1, 1, X, -1, -1}
//        };
    }



    public float step(@NotNull Action action) {

        // putting the stone for the player
        int col = action.getCol();
        int row = action.getRow();
        if (this.board[row][col] == 0) {
            this.board[row][col] = getPlayerToMove().getValue();
        } else {
            throw new RuntimeException("illegal Move");
        }

        float reward = reward();

    //    swapPlayer();

        return reward;
    }

    private float reward() {
        float reward = 0f;
//        if (hasPlayerWon(this.getPlayerToMove())) {
//            reward = 1f;
//        } else if (hasPlayerWon(OneOfTwoPlayer.otherPlayer(this.getPlayerToMove()))) {
//            reward = -1f;
//        }
        return reward;
    }



    private boolean isLegalAction(Action action_) {
        return legalActions().contains(action_);
    }


    public @NotNull List<Action> legalActions() {
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
        return false; //hasPlayerWon(OneOfTwoPlayer.PlayerA) || hasPlayerWon(OneOfTwoPlayer.PlayerB);
    }


    public @NotNull String render() {
        return render(config, preRender());
    }

    private static final String X_COORD = " A   B   C   D   E   F   G   H   I   J   K   L   M   N   O   P   Q   R   S   T   U   V   W   X   Y   Z ";
    private static final String X_COORD_SMALL = " A B C D E F G H I J K L M N O P Q R S T U V W X Y Z ";
    public int[][] board;
    public transient MuZeroConfig config;
    private OneOfTwoPlayer playerToMove;




    public static @NotNull String render(@NotNull MuZeroConfig config, String[][] values) {

        String v = "\u2502";


        boolean smallSpacing = true;
        if (values[0][0].length() > 1) {
            smallSpacing = false;
        }
        StringBuilder sb = new StringBuilder();
//        if (smallSpacing) {
//            sb.append("-".repeat(config.getSize() * 2 + 3));
//        } else {
//            sb.append("-".repeat(config.getSize() * 4 + 3));
//        }
        sb.append("\n");
        for (int j = 0; j < config.getBoardHeight(); j++) {
            sb.append(config.getBoardHeight() - j);
            sb.append(v);
            for (int i = 0; i < config.getBoardWidth(); i++) {
                String value = values[j][i];
                if (" 0%".equals(value)) {
                    value = "   ";
                }
                sb.append(value);
                sb.append(v);
            }
            sb.append("\n");
        }
        if (smallSpacing) {
            sb.append(" " + X_COORD_SMALL.substring(0, config.getSize() * 2) + "\n");
            //  sb.append("-".repeat(config.getSize()*2 + 2));
        } else {
            sb.append("  " + X_COORD.substring(0, config.getSize() * 4) + "\n");
            //   sb.append("-".repeat(config.getSize()*4 + 3));
        }


        return sb.toString();

    }


    @Override
    public @NotNull List<Action> allActionsInActionSpace() {
        throw new NotImplementedException("allActionsInActionSpace() not implemented, yet.");
    }

    public String[] @NotNull [] preRender() {
        String[][] values = new String[this.config.getBoardHeight()][config.getBoardWidth()];
        for (int j = this.config.getBoardHeight() - 1; j >= 0; j--) {
            for (int i = 0; i < config.getBoardWidth(); i++) {
                if (this.board[j][i] == OneOfTwoPlayer.PlayerA.getValue()) {
                    values[j][i] = String.valueOf(OneOfTwoPlayer.PlayerA.getSymbol());
                } else if (this.board[j][i] == OneOfTwoPlayer.PlayerB.getValue()) {
                    values[j][i] = String.valueOf(OneOfTwoPlayer.PlayerB.getSymbol());
                } else {
                    values[j][i] = " ";
                }
            }
        }
        return values;
    }


}
