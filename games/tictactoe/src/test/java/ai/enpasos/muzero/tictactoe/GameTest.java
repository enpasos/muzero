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

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class GameTest {




    @Autowired
    MuZeroConfig config;

    @Test
    void checkTerminal() {
        check( 1.0f, 1, 5, 0, 4, 7, 2, 8, 6);
        check(1.0f,1, 5, 0, 4, 7, 2, 8, 3);
        check(1.0f,1, 5, 0, 4, 2);
        check(1.0f,0, 5, 4, 3, 8);
        check(1.0f,2, 1, 4, 3, 6);
        check(1.0f,1, 2, 4, 3, 7);
        check(1.0f,3, 1, 4, 0, 5);
        check(1.0f,6, 1, 7, 0, 8);
        check(1.0f,0,5,2,6,3,8,4,7);
        check(0f,0,5,2,6,3,8,4,1,7);

    }


    private void check(float reward, int... actions) {
        Game game = config.newGame(true,true);
        for (int i = 0; i < actions.length; i++) {
            int a = actions[i];
            Objects.requireNonNull(game).apply(config.newAction(a));
            if (i == actions.length - 1) {
                assertTrue(game.terminal() || game.legalActions().size() == 0);
                assertEquals(reward, game.getReward());
                assertEquals(0,game.legalActions().size());
            } else {
                assertFalse(game.terminal());
            }
        }
    }
}
