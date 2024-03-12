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

import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
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
public class EntropyExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    Inference inference;

    public String listValuesForTrainedNetworks(List<Integer> actions) {

        StringWriter stringWriter = new StringWriter();


        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';').setHeader("t", "entropy", "actionIndex").build())) {
            IntStream.range(0, actions.size()).forEach(
                t -> {
                    try {
                        double entropy = inference.aiEntropy(actions.subList(0, t), config.getNetworkBaseDir());

                        csvPrinter.printRecord(t,
                            NumberFormat.getNumberInstance().format(entropy),
                            actions.get(t));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        } catch (IOException e) {
            throw new MuZeroException(e);
        }

        return stringWriter.toString();
    }

    @NotNull
    public List<Integer> getActionList() {


        gameBuffer.loadLatestStateIfExists();
        List<Game> gameList = gameBuffer.getPlanningBuffer().getEpisodeMemory().getGameList();
        Game game = gameList.get(gameList.size() - 1);

        List<Integer> actions = game.getEpisodeDO().getActions();
        log.debug(actions.toString());
        return actions;
    }

}
