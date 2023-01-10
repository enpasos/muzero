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

import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.SurpriseExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeSurpriseExtractor {
    @Autowired
    MuZeroConfig config;

    @Autowired
    SurpriseExtractor surpriseExtractor;

    @SuppressWarnings({"squid:S125", "CommentedOutCode"})
    public void run() {

       // Optional<Game> game = surpriseExtractor.getGame(4);
       //   Optional<Game> game = surpriseExtractor.getGameWithHighestSurprise();


     //   Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(4, 0, 8, 5, 6, 1, 3, 2);
       // Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(0, 6, 2, 4, 8, 3, 7, 5);


      //  game = surpriseExtractor.getGameStartingWithActionsFromStart(0, 4, 7, 3, 5, 6, 2, 1, 8);



    //    Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(8, 4, 7, 6, 2, 3, 5);


       // Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(8, 6, 0, 1, 5, 7, 3, 4);


       // Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(4, 5, 1, 7, 2, 3, 6);


        // a normal draw game (no mistake)
      //  Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(4, 0, 1, 7, 6, 2, 5, 3, 8);

        // a single mistake game
        // Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(4, 5, 8, 0, 6, 2, 7);


        // a double mistake game
           Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(4, 5, 8, 0, 6, 2, 3, 1);
        game.ifPresent(g -> System.out.println(surpriseExtractor.listValuesForTrainedNetworks(g)));

    }

}
