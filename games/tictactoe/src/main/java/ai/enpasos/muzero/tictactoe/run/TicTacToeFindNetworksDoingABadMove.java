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

import ai.enpasos.muzero.platform.agent.c_model.Inference;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.GameProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static ai.enpasos.muzero.platform.config.PlayTypeKey.PLAYOUT;
import static java.util.Map.entry;

@Slf4j
@SuppressWarnings({"squid:S106","java:S125"})
@Component
public class TicTacToeFindNetworksDoingABadMove {
    @Autowired
    MuZeroConfig config;


    @Autowired
    Inference inference;


    @Autowired
    GameProvider surpriseExtractor;


    public void run() {

        config.setPlayTypeKey(PLAYOUT);

        int start = 1;
        int stop = 1;   //1230;
        //config.setOutputDir("./memory/tictactoe-without-exploration/");
        config.setOutputDir("./memory/tictactoe/");


//        int start = 1;
//        int stop = 1230;
//        config.setOutputDir("./memory/tictactoe-with-exploration/");


        //   List<Integer> startingActions = List.of(4, 5, 8, 0, 6, 2, 3, 1);
        List<Integer> startingActions = List.of(4, 5, 8, 0, 6, 2);
        int nextBadAction = 3;

        //   findNetworkDoingBadAction(startingActions, nextBadAction, start, stop, false);
        findNetworkDoingBadAction(startingActions, nextBadAction, start, stop, true);


    }

    private void findNetworkDoingBadAction(List<Integer> startingActions, int nextBadAction, int start, int stop, boolean withMCTS) {
        for (int epoch = start; epoch <= stop; epoch++) {
            if (nextBadAction == inference.aiDecisionForGame(startingActions, withMCTS, Map.ofEntries(entry("epoch", epoch + "")))) {
                log.info("epoch with bad action {}, withMCTS {}", epoch, withMCTS);
            }
        }
    }

}
