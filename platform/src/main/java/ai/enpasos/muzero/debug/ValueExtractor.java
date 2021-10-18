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

package ai.enpasos.muzero.debug;

import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.List;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.go.agent.Inference.aiValue;

@Slf4j
public class ValueExtractor {

    public static String listValuesForTrainedNetworks(MuZeroConfig config, List<Integer> actions) throws IOException {




        //    List<Integer> actions = List.of(4, 2, 1, 8, 7, 0);
        //    List<Integer> actions = List.of(2, 4, 8, 1, 0, 7, 3);

        //    List<Integer> actions = List.of(12, 17, 10, 13, 8, 7, 18, 11, 14, 6, 12, 16, 13, 5, 22, 21, 2, 23, 1, 3, 4, 15, 9, 19, 24, 25, 22, 0, 23, 3, 2, 1, 25, 3, 25, 19, 23, 12, 4, 13, 8, 9, 22, 2, 18, 14, 25, 25);
        StringWriter stringWriter = new StringWriter();


        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.withDelimiter(';').withHeader("t", "vPlayerA", "actionIndex"))) {
            IntStream.range(0, actions.size()+1).forEach(
                    t -> {
                        try {
                            if (t == 24) {
                                int i = 42;
                            }
                            double value = aiValue(actions.subList(0, t), config.getNetworkBaseDir(), config);
                            double valuePlayerA = value * Math.pow(-1, t);
                            csvPrinter.printRecord(t,

                                    NumberFormat.getNumberInstance().format(valuePlayerA),
                                    t == 0 ? -1 : actions.get(t-1));

                        } catch (Exception e) {
                            // ignore
                        }
                    });

        }

        return stringWriter.toString();
    }

    @NotNull
    public static List<Integer> getActionList(MuZeroConfig config) {
        ReplayBuffer replayBuffer = new ReplayBuffer(config);

        replayBuffer.loadLatestState();


        // replayBuffer.getBuffer().getGames().forEach(g -> System.out.println(g.actionHistory().getActionIndexList()));


        Game game = replayBuffer.getBuffer().getGames().get( replayBuffer.getBuffer().getGames().size() - 1);

        List<Integer> actions = game.actionHistory().getActionIndexList();
        System.out.println(actions);
        return actions;
    }

}
