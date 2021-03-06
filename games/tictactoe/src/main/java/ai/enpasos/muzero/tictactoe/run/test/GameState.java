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

package ai.enpasos.muzero.tictactoe.run.test;

import ai.enpasos.muzero.platform.agent.memorize.ZeroSumGame;
import lombok.Data;

import java.util.Arrays;

@Data
public class GameState {

    private ZeroSumGame game;

    public GameState(ZeroSumGame game) {
        this.game = game;
    }

    public int hashCode() {
        return Arrays.deepHashCode(this.game.getEnvironment().getBoard());
    }

    public boolean equals(Object o) {
        if (!(o instanceof GameState)) return false;
        GameState gs = (GameState) o;
        return Arrays.deepEquals(this.game.getEnvironment().getBoard(), gs.game.getEnvironment().getBoard());
    }
}
