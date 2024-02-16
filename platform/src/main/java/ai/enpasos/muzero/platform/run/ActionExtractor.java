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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
public class ActionExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    Inference inference;


    @SuppressWarnings({"squid:S1141", "java:S106"})
    public void run() {

        gameBuffer.loadLatestStateIfExists();

        StringWriter stringWriter = new StringWriter();

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';').build())) {

            int count = 0;
            for (Game game : gameBuffer.getPlanningBuffer().getEpisodeMemory().getGameList()) {
                List<String> valueList = new ArrayList<>();
                valueList.add("" + count++);
                valueList.addAll(game.getEpisodeDO().getActions().stream().map(a -> a + "").collect(Collectors.toList()));

                String[] values = valueList.toArray(new String[0]);

                try {
                    csvPrinter.printRecord((Object[]) values);
                } catch (Exception e) {
                    // ignore
                }


            }

        } catch (IOException e) {
            throw new MuZeroException(e);
        }


        System.out.println(stringWriter);
        int count = 0;
        for (Game game : gameBuffer.getPlanningBuffer().getEpisodeMemory().getGameList()) {
            System.out.println(count++);
            System.out.println(game.render());
        }

    }


}
