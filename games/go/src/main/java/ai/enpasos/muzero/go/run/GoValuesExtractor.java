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
import ai.enpasos.muzero.platform.run.ValueExtractor;
import ai.enpasos.muzero.platform.run.ValuesExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class GoValuesExtractor {
    @Autowired
    MuZeroConfig config;

    @Autowired
    ValuesExtractor valuesExtractor;


    @Autowired
    ValueExtractor valueExtractor;

    @Autowired
    ReplayBuffer replayBuffer;


    @SuppressWarnings("squid:S125")
    public void run() {

        Game game = valuesExtractor.getGame();

        System.out.println(valuesExtractor.listValuesForTrainedNetworks(game));

//        System.out.println();
//        System.out.println();
//
//        List<Integer> actions = game.actionHistory().getActionIndexList();
//
//        System.out.println(valueExtractor.listValuesForTrainedNetworks(actions));

    }
}
