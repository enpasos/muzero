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

package ai.enpasos.muzero.go.run;

import ai.djl.util.Pair;
import ai.enpasos.muzero.go.ranking.Ranking;
import ai.enpasos.muzero.platform.agent.c_model.Inference;
import ai.enpasos.muzero.platform.agent.d_experience.GameBuffer;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
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
import java.util.stream.IntStream;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class GoStartValueExtractor {
    @Autowired
    MuZeroConfig config;

    @Autowired
    Inference inference;

    @Autowired
    GoElo goElo;


    @Autowired
    Ranking ranking;


    @Autowired
    GameBuffer gameBuffer;


    @SuppressWarnings("squid:S3740")
    public void run() {

        config.setNetworkBaseDir(config.getOutputDir() + "/networks");

        System.out.println(csvString(smoothing(valuesForTrainedNetworks(), 10)));

    }

    public List<Pair<Integer, Double>> smoothing(List<Pair<Integer, Double>> values, int n) {
        List<Pair<Integer, Double>> smoothedValues = new ArrayList<>();
        for (int k = 0; k < values.size(); k++) {
            int count = 0;
            double sum = 0d;
            for (int i = 0; i < n && k - i >= 0; i++) {
                count++;
                sum += values.get(k - i).getValue();
            }
            if (count == 0) throw new MuZeroException("count should not be zero");
            smoothedValues.add(new Pair<>(values.get(k).getKey(), sum / count));
        }
        return smoothedValues;
    }

    public List<Pair<Integer, Double>> valuesForTrainedNetworks() {
        goElo.assureThereIsSomeRankingForAllNetworks();
        int low = ranking.selectPlayerWithLowestEpoch();
        int high = ranking.selectPlayerWithHighestEpoch();

        List<Pair<Integer, Double>> values = new ArrayList<>();

        IntStream.rangeClosed(low, high).forEach(
            epoch -> {
                try {
                    double valuePlayerA = inference.aiStartValue(epoch);
                    values.add(new Pair<>(epoch, valuePlayerA));
                } catch (Exception e) {
                    // ignore
                }
            });


        return values;
    }


    public String csvString(List<Pair<Integer, Double>> values) {
        StringWriter stringWriter = new StringWriter();
        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';').setHeader("epoch", "vStart").build())) {
            values.forEach(
                pair -> {
                    try {
                        csvPrinter.printRecord(pair.getKey(),
                            NumberFormat.getNumberInstance().format(pair.getValue()));
                    } catch (Exception e) {
                        // ignore
                    }
                });

        } catch (IOException e) {
            throw new MuZeroException(e);
        }
        return stringWriter.toString();
    }


}
