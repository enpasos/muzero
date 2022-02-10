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

import ai.enpasos.muzero.platform.agent.rational.Player;
import ai.enpasos.muzero.platform.common.Constants;
import org.jetbrains.annotations.NotNull;


public enum OneOfTwoPlayer implements Player {

    PLAYER_A(-1, Constants.CHAR_PLAYER_A, 0f), PLAYER_B(1, Constants.CHAR_PLAYER_B, 1f);

    private int value;
    private char symbol;
    private float actionValue;

    OneOfTwoPlayer(int value, char symbol, float actionValue) {
        this.value = value;
        this.symbol = symbol;
        this.actionValue = actionValue;
    }

    public static @NotNull OneOfTwoPlayer otherPlayer(OneOfTwoPlayer player) {
        if (player == PLAYER_A) return PLAYER_B;
        else return PLAYER_A;
    }


    public char getSymbol() {
        return symbol;
    }


    public int getValue() {
        return value;
    }


    public float getActionValue() {
        return actionValue;
    }


}

