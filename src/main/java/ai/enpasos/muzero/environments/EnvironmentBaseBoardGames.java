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

package ai.enpasos.muzero.environments;


import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.play.Action;
import lombok.Data;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;


@Data
public class EnvironmentBaseBoardGames implements Environment, Serializable {


    public int[][] board;
    public transient MuZeroConfig config;

   private OneOfTwoPlayer playerToMove;


    public EnvironmentBaseBoardGames(@NotNull MuZeroConfig config) {
        this.config = config;
        board = new int[config.getBoardHeight()][config.getBoardWidth()];
        playerToMove = OneOfTwoPlayer.PlayerA;
    }

    public static @NotNull String render(@NotNull MuZeroConfig config, String[][] values) {

        String v = "\u2502";



        boolean smallSpacing = true;
        if (values[0][0].length() > 1) {
            smallSpacing = false;
        }
        StringBuilder sb = new StringBuilder();
        if (smallSpacing) {
            sb.append("-".repeat(config.getSize() * 2 + 3));
        } else {
            sb.append("-".repeat(config.getSize() * 4 + 3));
        }
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
            sb.append("-".repeat(config.getSize()*2 + 2));
        } else {
            sb.append("  " + X_COORD.substring(0, config.getSize() * 4) + "\n");
            sb.append("-".repeat(config.getSize()*4 + 3));
        }



        return sb.toString();

    }


    private static final String X_COORD = " A   B   C   D   E   F   G   H   I   J   K   L   M   N   O   P   Q   R   S   T   U   V   W   X   Y   Z ";
    private static final String X_COORD_SMALL = " A B C D E F G H I J K L M N O P Q R S T U V W X Y Z ";


    public float step(Action action) {
        throw new NotImplementedException("step() not implemented, yet.");
    }

    @Override
    public int[][] currentImage() {
        throw new NotImplementedException("currentImage() not implemented, yet.");
    }

    @Override
    public boolean terminal() {
        throw new NotImplementedException("terminal() not implemented, yet.");
    }

    @Override
    public @NotNull List<Action> legalActions() {
        throw new NotImplementedException("legalActions() not implemented, yet.");
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

    protected void swapPlayer() {
        this.playerToMove = OneOfTwoPlayer.otherPlayer(this.playerToMove);
    }

    public boolean hasPlayerWon(OneOfTwoPlayer player) {
        throw new NotImplementedException("hasPlayerWon is not yet implemented");
    }


}
