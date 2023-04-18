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

import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.ValueExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class GoValueExtractor {
    @Autowired
    MuZeroConfig config;

    @Autowired
    ValueExtractor valueExtractor;

    @Autowired
    GameBuffer gameBuffer;


    @SuppressWarnings("squid:S125")
    public void run() {

       // List<Integer> actionIndexList = valueExtractor.getActionList();
        List<Integer> actionIndexList = List.of(12, 16, 17, 6, 11, 7, 13, 3, 8, 10, 22, 19, 15, 20, 21, 15, 5, 15, 1, 18, 2, 14, 7);

        System.out.println(valueExtractor.listValuesForTrainedNetworks(actionIndexList));

    }

}
