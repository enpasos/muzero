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

package ai.enpasos.muzero.go.debug;


import ai.enpasos.muzero.go.config.GoConfigFactory;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;

import static ai.enpasos.muzero.platform.debug.RenderGame.applyAction;
import static ai.enpasos.muzero.platform.debug.RenderGame.renderGame;


public class RenderGame {

    public static void main(String[] args) {

        MuZeroConfig config = GoConfigFactory.getGoInstance(5);

        Game game = config.newGame();
        applyAction(game, 0);
        applyAction(game, 5);
        applyAction(game, 8);
        applyAction(game, 7);
        applyAction(game, 3);
        applyAction(game, 1);
        applyAction(game, 2);
        applyAction(game, 6);
        applyAction(game, 4);

        renderGame(config, game);
    }
}
