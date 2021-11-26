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

     //   List<Integer> actions = List.of(108, 64, 24, 29, 178, 101, 165, 102, 77, 112, 180, 178, 45, 64, 100, 46, 164, 80, 166, 44, 181, 78, 32, 66, 174, 167, 116, 179, 30, 163);
        List<Integer> actions = List.of(108, 64, 29, 178, 44, 78, 186, 171, 193, 44, 173, 178, 109, 167, 25, 77, 34, 180, 102, 178, 70, 100, 23, 63, 24, 107, 101, 165, 114, 78);
        Game game = config.newGame();
        actions.stream().forEach(
                a -> {
                    applyAction(game, a);

                });

        renderGame(config, game);
    }
}
