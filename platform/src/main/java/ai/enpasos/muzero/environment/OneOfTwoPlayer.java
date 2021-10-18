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

package ai.enpasos.muzero.environment;

import ai.enpasos.muzero.agent.slow.play.Player;
import org.jetbrains.annotations.NotNull;


public enum OneOfTwoPlayer implements Player {

    PlayerA(-1, 'x', 0f), PlayerB(1, 'o', 1f);

    private int value;
    private char symbol;
    private float actionValue;

    OneOfTwoPlayer(int value, char symbol, float actionValue) {
        this.setValue(value);
        this.setSymbol(symbol);
        this.setActionValue(actionValue);
    }

    public static @NotNull OneOfTwoPlayer otherPlayer(OneOfTwoPlayer player) {
        if (player == PlayerA) return PlayerB;
        else return PlayerA;
    }


    public char getSymbol() {
        return symbol;
    }

    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public float getActionValue() {
        return actionValue;
    }

    public void setActionValue(float actionValue) {
        this.actionValue = actionValue;
    }
}

