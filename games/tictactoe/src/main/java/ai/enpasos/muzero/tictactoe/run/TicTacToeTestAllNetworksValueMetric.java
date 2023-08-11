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

import ai.djl.util.Pair;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.tictactoe.run.exploitability.GameTree;
import ai.enpasos.muzero.tictactoe.run.exploitability.TicTacToeTestExploitability;
import ai.enpasos.muzero.tictactoe.run.exploitability.TicTacToeTestValueMetric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeTestAllNetworksValueMetric {
    @Autowired
    MuZeroConfig config;

    @Autowired
    TicTacToeTestValueMetric test;


    @SuppressWarnings({"squid:S125", "CommentedOutCode"})
    public void run() {


        int start = 300;
        int stop = 300;
        boolean leafsOnly = false;


       // config.setOutputDir("./memory/tictactoe-without-exploration/");
        //config.setOutputDir("./memory/tictactoe-with-exploration/");

        Map<Integer, Double> map = new TreeMap<>();
        GameTree gameTree = test.prepareGameTree();

        for (int epoch = start; epoch <= stop; epoch++) {
            System.out.println("epoch: " + epoch);
            map.put(epoch, test.valueMetric(epoch, gameTree, leafsOnly));
        }

        NumberFormat nf= NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        System.out.println("epoch;valueMetric");
        for (Map.Entry<Integer,  Double> entry : map.entrySet()) {
            double result = entry.getValue();
            System.out.println(entry.getKey() + ";" + nf.format(result));
        }


    }

}
