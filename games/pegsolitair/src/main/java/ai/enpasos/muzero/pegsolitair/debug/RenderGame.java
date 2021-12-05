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

    @SuppressWarnings("CommentedOutCode")
    public static void main(String[] args) {

        MuZeroConfig config = PegSolitairConfigFactory.getSolitairInstance();

        // perfect: List<Integer> actions = List.of(71, 107, 165, 102, 51, 25, 174, 102, 23, 37, 25, 39, 77, 178, 112, 77, 121, 44, 193, 181, 123, 167, 109, 172, 30, 107, 186, 44, 30, 64, 108);
        //  perfect: List<Integer> actions = List.of(173, 109, 167, 34, 25, 102, 39, 25, 51, 102, 66, 167, 16, 63, 23, 86, 70, 121, 100, 77, 37, 23, 58, 46, 122, 109, 93, 123, 46, 180, 38);
        List<Integer> actions = List.of(71, 172, 109, 58, 102, 23, 100, 173, 39, 79, 77, 37, 171, 123, 46, 70, 93, 23, 63, 107, 165, 23, 180, 38, 167, 34);


        Game game = config.newGame();
        actions.forEach(
                a -> applyAction(game, a));

        renderGame(config, game);
    }
}
