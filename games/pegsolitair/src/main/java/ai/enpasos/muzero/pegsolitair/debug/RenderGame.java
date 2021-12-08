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


import ai.enpasos.muzero.pegsolitair.config.PegSolitairConfigFactory;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;

import java.util.List;

import static ai.enpasos.muzero.platform.debug.RenderGame.applyAction;
import static ai.enpasos.muzero.platform.debug.RenderGame.renderGame;


public class RenderGame {

    @SuppressWarnings("squid:S125")
    public static void main(String[] args) {

        MuZeroConfig config = PegSolitairConfigFactory.getSolitairInstance();

        // perfect games:
        // List<Integer> actions = List.of(71, 107, 165, 102, 51, 25, 174, 102, 23, 37, 25, 39, 77, 178, 112, 77, 121, 44, 193, 181, 123, 167, 109, 172, 30, 107, 186, 44, 30, 64, 108);
        //  List<Integer> actions = List.of(173, 109, 167, 34, 25, 102, 39, 25, 51, 102, 66, 167, 16, 63, 23, 86, 70, 121, 100, 77, 37, 23, 58, 46, 122, 109, 93, 123, 46, 180, 38);
        // List<Integer> actions = List.of(71, 37, 171, 186, 77, 37, 113, 112, 123, 174, 116, 167, 77, 121, 107, 39, 66, 181, 102, 123, 51, 44, 121, 193, 44, 79, 39, 25, 102, 166, 108);
        //  List<Integer> actions = List.of(71, 37, 171, 186, 70, 114, 37, 77, 63, 121, 107, 44, 121, 193, 123, 181, 44, 118, 39, 117, 66, 181, 102, 123, 79, 39, 25, 51, 102, 166, 108);

        List<Integer> actions = List.of(71, 107, 63, 23, 28, 37, 186, 171, 29, 63, 107, 44, 121, 193, 123, 174, 116, 44, 167, 66, 102, 51, 39, 181, 123, 79, 39, 25, 102, 166, 108);


        Game game = config.newGame();
        actions.forEach(
                a -> applyAction(game, a));

        renderGame(config, game);
    }
}
