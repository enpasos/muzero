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


import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.debug.RenderGame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class GoRenderGame {

    @Autowired
    MuZeroConfig config;

    @Autowired
    RenderGame renderGame;

    public void run() {

        List<Integer> actions = List.of(71, 172, 39, 38, 78, 44, 121, 181, 193, 80, 44, 118, 115, 166, 178, 77, 102, 181, 25, 108, 51, 102, 64, 66, 112, 117, 180, 178, 77);


        Game game = config.newGame();
        actions.forEach(
                a -> renderGame.applyAction(game, a));

        renderGame.renderGame(game);
    }
}
