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

import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeInMindValues {
    @Autowired
    MuZeroConfig config;


    @Autowired
    Inference inference;


    @SuppressWarnings({"squid:S125", "CommentedOutCode"})
    public void run() {


        // a multi-mistake game
        int[] actions = {0, 3, 1, 4, 5, 7, 8, 6};
        int epoch = 1250;
        int extra = 10;
        //     config.setOutputDir("./memory/tictactoe-without-exploration/");
        //  config.setOutputDir("./memory/tictactoe/");


        double[][] values = inference.getInMindValues(epoch, actions, extra, config.getActionSpaceSize());

        System.out.println(output(values, epoch));

    }

    @SuppressWarnings("squid:S1141")
    public String output(double[][] values, int epoch) {
        System.out.println("epoch: " + epoch);
        StringWriter stringWriter = new StringWriter();
        List<String> header = new ArrayList<>();
        header.add("t/t'");
        for (int i = 0; i < values.length; i++) {
            header.add(String.valueOf(i));
        }
        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';').setHeader(header.toArray(new String[0])).build())) {

            for (int t = 0; t < values[0].length; t++) {
                Object[] objects = new Object[values.length + 1];
                objects[0] = t;
                for (int r = 0; r < values.length; r++) {


                    double valuePlayer = values[r][t];
                    if (config.getPlayerMode() == PlayerMode.TWO_PLAYERS) {
                        valuePlayer *= Math.pow(-1, t);
                    }
                    objects[1 + r] = NumberFormat.getNumberInstance().format(valuePlayer);
                }
                try {
                    csvPrinter.printRecord(objects);
                } catch (Exception e) {
                    // ignore
                }
            }

        } catch (IOException e) {
            throw new MuZeroException(e);
        }

        return stringWriter.toString();
    }
}
