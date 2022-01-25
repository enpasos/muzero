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

package ai.enpasos.muzero.platform.debug;

import ai.enpasos.muzero.platform.agent.Inference;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.List;
import java.util.stream.IntStream;


@Slf4j
@Component
public class ValueExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    ReplayBuffer replayBuffer;

    @Autowired
    Inference inference;

    public String listValuesForTrainedNetworks(List<Integer> actions) {


        StringWriter stringWriter = new StringWriter();


        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.withDelimiter(';').withHeader("t", "vPlayerA", "actionIndex"))) {
            IntStream.range(0, actions.size() + 1).forEach(
                    t -> {
                        try {
                            double valuePlayerA = inference.aiValue(actions.subList(0, t), config.getNetworkBaseDir());
                            if (config.getPlayerMode() == PlayerMode.TWO_PLAYERS) {
                                valuePlayerA *= Math.pow(-1, t);
                            }
                            csvPrinter.printRecord(t,
                                    NumberFormat.getNumberInstance().format(valuePlayerA),
                                    t == 0 ? -1 : actions.get(t - 1));

                        } catch (Exception e) {
                            // ignore
                        }
                    });

        } catch (IOException e) {
            throw new MuZeroException(e);
        }

        return stringWriter.toString();
    }

    @NotNull
    public List<Integer> getActionList() {


        replayBuffer.loadLatestState();

        Game game = replayBuffer.getBuffer().getGames().get(replayBuffer.getBuffer().getGames().size() - 3);

        List<Integer> actions = game.actionHistory().getActionIndexList();
        log.debug(actions.toString());
        return actions;
    }

}
