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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("ConstantConditions")

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class EnvironmentTest {

    @Autowired
    MuZeroConfig config;

    @Test
    void checkIfPlayerHasWon() {
        Game game = config.newGame(true,true);
        Objects.requireNonNull(game).apply(0, 3, 1, 4, 2);
        assertEquals(1f, game.getLastReward(), 0.0);

        game = config.newGame(true,true);
        Objects.requireNonNull(game).apply(0, 1, 3, 4, 2, 5, 7, 6, 8);
        assertEquals(0f, game.getLastReward(), 0.0);

        game = config.newGame(true,true);
        Objects.requireNonNull(game).apply(0, 1, 2, 4, 8, 7);
        assertEquals(1f, game.getLastReward(), 0.0);
    }
}
