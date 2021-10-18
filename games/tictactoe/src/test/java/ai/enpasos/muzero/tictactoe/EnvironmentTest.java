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

package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.tictactoe.config.ConfigFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Objects;

class EnvironmentTest {

    @Test
    void checkIfPlayerHasWon() {
        MuZeroConfig config = ConfigFactory.getTicTacToeInstance();
        Game game = config.newGame();
        Objects.requireNonNull(game).apply(0, 3, 1, 4, 2);
        Assert.assertEquals(game.getLastReward(), 1f, 0.0);

        game = config.newGame();
        Objects.requireNonNull(game).apply(0, 1, 3, 4, 2, 5, 7, 6, 8);
        Assert.assertEquals(game.getLastReward(), 0f, 0.0);
    }
}
