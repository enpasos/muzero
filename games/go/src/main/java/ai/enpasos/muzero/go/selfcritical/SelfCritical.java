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

package ai.enpasos.muzero.go.selfcritical;

import ai.enpasos.muzero.go.config.GoGame;
import ai.enpasos.muzero.platform.agent.gamebuffer.*;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@SuppressWarnings("squid:S106")
public class SelfCritical {


    @Autowired
    private ReplayBuffer replayBuffer;

    @Autowired
    private SelfCriticalTrain  train;

    @Autowired
    private SelfCriticalTest test;

    public void run() {

        // assuming that the buffer is filled only by data produced by one (the latest) network
        replayBuffer.loadLatestState();


      int numOfGames = replayBuffer.getBuffer().getData().size();

        List<SelfCriticalLabeledFeature> rawFeatures = new ArrayList<>();

        SelfCriticalDataSet dataSet = new SelfCriticalDataSet();

        // int g = replayBuffer.getBuffer().getGames().size() - 3;
        for(int g = 0; g < numOfGames; g++) {
            Game game = replayBuffer.getBuffer().getGames().get(g);
            boolean trusted = true;
            for(int a = game.getGameDTO().getActions().size()-1; a >= 0; a--) {
                SelfCriticalLabeledFeature feature = SelfCriticalLabeledFeature.builder()
                    .numberOfMovesPlayedSofar(a + 1)
                    .winner(((GoGame)game).whoWonTheGame().get() )
                    .value(game.getGameDTO().getRootValuesFromInitialInference().get(a))
                    .toPlay( (a % 2 == 0) ?  OneOfTwoPlayer.PLAYER_A : OneOfTwoPlayer.PLAYER_B)
                    .build();
                feature.transformRawToPreNormalizedInput();
                if (!feature.correct) {
                    trusted = false;
                }
                feature.setCorrectAndNoMindChange(trusted && feature.correct);
                rawFeatures.add(feature);
            }
        }

        dataSet.features = rawFeatures;


//        Collections.reverse( dataSet.features);
//        dataSet.features.stream().forEach(f -> System.out.println(f.correctAndNoMindChange ? 1f : -1f));
//        System.out.println("");


        dataSet.transformRawToNormalizedInput();

//        dataSet.features.stream().forEach(f -> System.out.println(f.value));
//        System.out.println("");

//        dataSet.features.stream().forEach(f -> System.out.println(f.correct));
//        System.out.println("");

        long correctN = dataSet.features.stream()
            .filter(f -> f.correct).count();

        long notCorrectN = dataSet.features.stream()
            .filter(f -> !f.correct).count();

        System.out.println("correct: " + correctN + ", not correct: " + notCorrectN);

if (notCorrectN > 0 ) {
    double entropy = dataSet.features.stream()
        .filter(f -> !f.correct)
        .min(Comparator.comparing(SelfCriticalLabeledFeature::getEntropy))
        .get().getEntropy();


    System.out.println("below this entropy only correct guesses: " + entropy);


        long countReliableDataPoints = dataSet.features.stream()
            .filter(f -> f.entropy < entropy).count();

        long countAllDataPoints = dataSet.features.stream()
            .count();

        System.out.println("reliable data points " + countReliableDataPoints + " out of " + countAllDataPoints);
}

//        dataSet.features.stream().forEach(f -> System.out.println(f.correct ? 1 : 0));
//        System.out.println("");
//
//        dataSet.features.stream().forEach(f -> System.out.println(f.value));
//        System.out.println("");
        try {
            train.run(dataSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Float>  testResult = test.run(dataSet.features);
        testResult.stream().forEach(f -> System.out.println(f));
//        System.out.println("");
//        long numberOK = testResult.stream().filter(f -> f.booleanValue()).count();
//        long numberNOK = testResult.stream().filter(f -> !f.booleanValue()).count();
//        log.info("ok: " + numberOK + ", nok: " + numberNOK);
    }

}
