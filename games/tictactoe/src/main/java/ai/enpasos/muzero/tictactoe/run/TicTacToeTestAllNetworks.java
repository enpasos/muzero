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

import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.tictactoe.run.test.BadDecisions;
import ai.enpasos.muzero.tictactoe.run.test.GameTree;
import ai.enpasos.muzero.tictactoe.run.test.TicTacToeTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeTestAllNetworks {
    @Autowired
    MuZeroConfig config;

    @Autowired
    TicTacToeTest test;


    @SuppressWarnings({"squid:S125", "CommentedOutCode"})
    public void run() {


        int start = 286;
        int stop = 385;

        boolean onOptimalPathOnly = false;

       // config.setOutputDir("./memory/tictactoe-without-exploration/");
        //config.setOutputDir("./memory/tictactoe-with-exploration/");

        Map<Integer, BadDecisions> map = new TreeMap<>();
        GameTree gameTree = test.prepareGameTree();

        for (int epoch = start; epoch <= stop; epoch++) {
            map.put(epoch, test.findBadDecisions(epoch, gameTree, onOptimalPathOnly));
        }


        System.out.println("epoch;total;withoutMCTS;withMCTS");
        for (Map.Entry<Integer, BadDecisions> entry : map.entrySet()) {
            BadDecisions bd = entry.getValue();
            System.out.println(entry.getKey() + ";" + bd.total() + ";" + bd.getModelBased() + ";" + bd.getPlanningBased());
        }


    }

}
