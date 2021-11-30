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

package ai.enpasos.muzero.pegsolitair.debug;

import ai.djl.util.Pair;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.pegsolitair.config.PegSolitairConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.debug.ValueExtractor.getActionList;
import static ai.enpasos.muzero.platform.debug.ValueExtractor.listValuesForTrainedNetworks;

@Slf4j
public class ValueExtractor {

    public static void main(String[] args) throws IOException {

        MuZeroConfig config = PegSolitairConfigFactory.getSolitairInstance();
        config.setNetworkBaseDir(config.getOutputDir() + "/networks");

        List<Integer> actionIndexList = getActionList(config);

          System.out.println(listValuesForTrainedNetworks(config, actionIndexList));



        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        replayBuffer.loadLatestState();
        List<Pair> pairs = replayBuffer.getBuffer().getGames().stream().map(g -> new Pair(g.actionHistory().getActionIndexList(), g.getLastReward()))
                .sorted((p1,p2) -> {
                    int comp = ((Float)p1.getValue()).compareTo(((Float)p2.getValue()));
                    if (comp != 0) return comp;
                    return ((List<Integer>)p1.getKey()).toString().compareTo(((List<Integer>)p2.getKey()).toString());
                })
                .collect(Collectors.toList());

        pairs.forEach(p -> {
          System.out.println(p.getKey() + "; " + p.getValue());

        });


    }

}
