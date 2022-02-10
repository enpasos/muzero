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

package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.agent.memory.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.RenderGame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class TicTacToeRenderGame {

    @Autowired
    MuZeroConfig config;

    @Autowired
    RenderGame renderGame;

    public void run() {


        Game game = config.newGame();
        renderGame.applyAction(game, 0);
        renderGame.applyAction(game, 5);
        renderGame.applyAction(game, 8);
        renderGame.applyAction(game, 7);
        renderGame.applyAction(game, 3);
        renderGame.applyAction(game, 1);
        renderGame.applyAction(game, 2);
        renderGame.applyAction(game, 6);
        renderGame.applyAction(game, 4);

        renderGame.renderGame(game);
    }
}
