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

package ai.enpasos.muzero.platform.run;

import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
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


@Slf4j
@Component
public class SurpriseExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    ReplayBuffer replayBuffer;

    @Autowired
    Inference inference;

    public String listValuesForTrainedNetworks(Game game) {

        List<Float> values = game.getGameDTO().getRootValuesFromInitialInference();
        StringWriter stringWriter = new StringWriter();

        List<Float> surprises = game.getGameDTO().getSurprises();

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';').setHeader("t", "surprise", "vPlayerA").build())) {
            for (int t = 0; t < values.size(); t++) {

                float value = values.get(t);
                float surprise = surprises.get(t);
                try {
                    double valuePlayer = value;
                    if (config.getPlayerMode() == PlayerMode.TWO_PLAYERS) {
                        valuePlayer *= Math.pow(-1, t);
                    }
                    csvPrinter.printRecord(t,
                        NumberFormat.getNumberInstance().format(surprise),
                        NumberFormat.getNumberInstance().format(valuePlayer));
                } catch (Exception e) {
                    // ignore
                }

            }

        } catch (IOException e) {
            throw new MuZeroException(e);
        }

        return stringWriter.toString();
    }

    @NotNull
    public Game getGame() {


        replayBuffer.loadLatestState();


        return replayBuffer.getBuffer().getGames().get(replayBuffer.getBuffer().getGames().size() - 1);


    }

}
