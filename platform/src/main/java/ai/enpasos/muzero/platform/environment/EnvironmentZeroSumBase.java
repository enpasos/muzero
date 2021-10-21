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

package ai.enpasos.muzero.platform.environment;


import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import lombok.Data;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;


@Data
public class EnvironmentZeroSumBase extends EnvironmentBase {

    public EnvironmentZeroSumBase(MuZeroConfig config) {
        super(config);
        playerToMove = OneOfTwoPlayer.PlayerA;
    }

    private OneOfTwoPlayer playerToMove;

    protected void swapPlayer() {
        this.playerToMove = OneOfTwoPlayer.otherPlayer(this.playerToMove);
    }

    public boolean hasPlayerWon(OneOfTwoPlayer player) {
        throw new NotImplementedException("hasPlayerWon is not yet implemented");
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
