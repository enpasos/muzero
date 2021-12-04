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

package ai.enpasos.muzero.platform.debug;

import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RenderGame {


    public static void applyAction(@NotNull Game game, int a) {
        game.apply(a);

        System.out.println("action=" + a + ", terminal=" + game.terminal() + ", " + game.legalActions() + ", lastreward=" + game.getLastReward());
    }

    public static void renderGame(@NotNull MuZeroConfig config, @NotNull Game game) {
        Game replayGame = config.newGame();

        System.out.println("\n" + Objects.requireNonNull(replayGame).render());
        for (int i = 0; i < game.getGameDTO().getActionHistory().size(); i++) {
            Action action = config.newAction(game.getGameDTO().getActionHistory().get(i));


            replayGame.apply(action);
            System.out.println("\n" + replayGame.render());
        }
        System.out.println(game.getEnvironment().toString());
    }

}
