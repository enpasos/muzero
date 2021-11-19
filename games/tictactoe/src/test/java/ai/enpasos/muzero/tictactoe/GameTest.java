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

import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.tictactoe.config.TicTacToeConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.Objects;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

public class GameTest {

    @Test
    public void checkIfPlayerHasWon() {
        check(new int[]{1, 5, 0, 4, 7, 2, 8, 6});
        check(new int[]{1, 5, 0, 4, 7, 2, 8, 3});
        check(new int[]{1, 5, 0, 4, 2});
        check(new int[]{0, 5, 4, 3, 8});
    }

    private void check(int @NotNull [] actions) {
        MuZeroConfig config = TicTacToeConfigFactory.getTicTacToeInstance();
        Game game = config.newGame();
        for (int i = 0; i < actions.length; i++) {
            int a = actions[i];
            Objects.requireNonNull(game).apply(new Action(config, a));
            if (i == actions.length - 1) {
                assertTrue(game.terminal());
            } else {
                assertFalse(game.terminal());
            }
        }
    }
}
