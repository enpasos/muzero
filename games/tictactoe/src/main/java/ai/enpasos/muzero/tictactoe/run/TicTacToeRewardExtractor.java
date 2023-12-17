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
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBufferIO;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.run.GameProvider;
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
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeRewardExtractor {
    @Autowired
    MuZeroConfig config;

    @Autowired
    GameProvider gameProvider;

    @Autowired
    GameBufferIO replayBufferIO;

    @Autowired
    Inference inference;

    @SuppressWarnings({"squid:S125", "CommentedOutCode"})
    public void run() {


        // a double mistake game
        int[] actions = {4, 5, 8, 0, 6, 2, 3, 1};
        int start = 0;
        int stop =  replayBufferIO.getLatestNetworkEpoch();

        //   Optional<Game> game = surpriseExtractor.getGameStartingWithActionsFromStart(4, 5, 8, 0, 6, 2, 3, 1);

        double[][] rewards = new double[stop - start + 1][actions.length];
        for (int epoch = start; epoch <= stop; epoch++) {
            rewards[epoch-start] = inference.getInRewards(epoch, actions);
        }


        System.out.println(listRewardsForTrainedNetworks(rewards, start, stop, actions.length));

    }

    @SuppressWarnings("squid:S1141")
    public String listRewardsForTrainedNetworks(double[][] rewards, int start, int stop, int numValues) {

        StringWriter stringWriter = new StringWriter();
        List<String> header = new ArrayList<>();
        header.add("t/epoch");
        IntStream.rangeClosed(start, stop).forEach(epoch -> header.add(epoch + ""));

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';').setHeader(header.toArray(new String[0])).build())) {

            for (int t = 0; t < numValues; t++) {
                Object[] objects = new Object[stop - start + 2];
                objects[0] = t;
                for (int epoch = start; epoch <= stop; epoch++) {
                    double valuePlayer = rewards[epoch - start][t];
                    if (config.getPlayerMode() == PlayerMode.TWO_PLAYERS) {
                        valuePlayer *= Math.pow(-1, t);
                    }
                    objects[1 + epoch - start] = NumberFormat.getNumberInstance().format(valuePlayer);
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
