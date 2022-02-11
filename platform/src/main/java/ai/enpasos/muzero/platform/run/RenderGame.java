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

package ai.enpasos.muzero.platform.run;

import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class RenderGame {

    @Autowired
    MuZeroConfig config;

    public void applyAction(@NotNull Game game, int a) {
        game.apply(a);

        log.debug("action=" + a + ", terminal=" + game.terminal() + ", " + game.legalActions() + ", lastreward=" + game.getLastReward());
    }

    public void renderGame(@NotNull Game game) {
        Game replayGame = config.newGame();

        log.debug("\n" + Objects.requireNonNull(replayGame).render());
        for (int i = 0; i < game.getGameDTO().getActions().size(); i++) {
            Action action = config.newAction(game.getGameDTO().getActions().get(i));


            replayGame.apply(action);
            log.debug("\n" + replayGame.render());
        }
        log.debug(game.getEnvironment().toString());
    }

}
