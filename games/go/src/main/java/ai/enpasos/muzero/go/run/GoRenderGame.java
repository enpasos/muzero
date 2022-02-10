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

package ai.enpasos.muzero.go.run;


import ai.enpasos.muzero.platform.agent.memory.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.RenderGame;
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

        List<Integer> actions = List.of(12, 17, 13, 11, 6, 16, 18, 7, 8, 5, 2, 1, 7, 3, 0, 9, 4, 1, 14, 19, 0, 23, 10, 15, 21, 5, 24, 1, 22, 10, 0, 25, 20, 11, 17, 25, 16, 25, 15, 25, 10, 25, 1, 25, 25);


        Game game = config.newGame();
        actions.forEach(
                a -> renderGame.applyAction(game, a));

        renderGame.renderGame(game);
    }
}
