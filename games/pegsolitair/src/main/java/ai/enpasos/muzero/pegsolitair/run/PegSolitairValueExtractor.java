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

package ai.enpasos.muzero.pegsolitair.run;

import ai.djl.util.Pair;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.ValueExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings({"squid:S106", "unchecked"})
@Component
public class PegSolitairValueExtractor {
    @Autowired
    MuZeroConfig config;

    @Autowired
    ValueExtractor valueExtractor;

    @Autowired
    GameBuffer gameBuffer;

    @SuppressWarnings("squid:S3740")
    public void run() {

        config.setNetworkBaseDir(config.getOutputDir() + "/networks");

        List<Integer> actionIndexList = valueExtractor.getActionList();

        System.out.println(valueExtractor.listValuesForTrainedNetworks(actionIndexList));

        gameBuffer.loadLatestStateIfExists();

        List<Pair> pairs = gameBuffer.getBuffer().getGames().stream().map(g -> new Pair(g.getGameDTO().getActions(), g.getReward()))
            .sorted(Comparator.comparing((Pair p) -> ((Float) p.getValue())).thenComparing(p -> p.getKey().toString()))
            .collect(Collectors.toList());

        pairs.forEach(p -> System.out.println(p.getKey() + "; " + p.getValue()));

    }

}
