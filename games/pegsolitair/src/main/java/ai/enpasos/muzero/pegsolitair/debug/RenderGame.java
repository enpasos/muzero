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

package ai.enpasos.muzero.pegsolitair.debug;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.pegsolitair.config.PegSolitairConfigFactory;

import java.util.List;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.debug.RenderGame.applyAction;
import static ai.enpasos.muzero.platform.debug.RenderGame.renderGame;


public class RenderGame {

    public static void main(String[] args) {

        MuZeroConfig config = PegSolitairConfigFactory.getSolitairInstance();

        List<Integer> actions = List.of(173, 39, 181, 80, 115, 166, 86, 118, 46, 108, 32, 122, 93, 181, 121, 70, 109, 114, 37, 123, 100, 46, 23, 77, 151, 63, 107, 121, 86, 39);
        Game game = config.newGame();
        actions.stream().forEach(
                a -> {
                    applyAction(game, a);

                });

        renderGame(config, game);
    }
}
