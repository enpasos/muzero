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

import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.SurpriseExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class GoSurpriseExtractor {
    @Autowired
    MuZeroConfig config;

    @Autowired
    SurpriseExtractor surpriseExtractor;

    @Autowired
    ReplayBuffer replayBuffer;


    @SuppressWarnings("squid:S125")
    public void run() {

//        Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(
//        12, 17, 11, 13, 7, 8, 6, 16, 18, 19, 1, 23, 0, 15, 20, 21, 3, 9, 10, 4, 2, 5, 6, 11, 0, 10, 7, 1, 2, 0, 3, 12, 6, 7, 2, 3, 25
//        );
//        Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(
//            12, 16, 18, 20, 7, 24
//        );




        Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(
            12, 17, 11, 13, 7, 19, 6, 23, 8, 14, 3, 22, 1, 16, 15, 9, 5, 21, 4, 20, 10, 25, 0, 2, 12
        );

      //  Optional<Game> game = surpriseExtractor.getGame();
        game.ifPresent(g -> System.out.println(surpriseExtractor.listValuesForTrainedNetworks(g)));
    }

}
