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
import org.jetbrains.annotations.NotNull;
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

       long nWinA = replayBuffer.getBuffer().getGames().stream().filter(g -> (((GoGame)g).whoWonTheGame().get() == OneOfTwoPlayer.PLAYER_A)).count();
        long nWinB = replayBuffer.getBuffer().getGames().stream().filter(g -> (((GoGame)g).whoWonTheGame().get() == OneOfTwoPlayer.PLAYER_B)).count();



        int numOfGames = replayBuffer.getBuffer().getData().size();



        SelfCriticalDataSet dataSet = getSelfCriticalDataSet(0, numOfGames-1);


        //     Collections.reverse( dataSet.features);
//        dataSet.features.stream().forEach(f -> System.out.println(f.correctAndNoMindChange ? 1f : -1f));
//        System.out.println("");


     //   dataSet.transformRawToNormalizedInput();

//        dataSet.features.stream().forEach(f -> System.out.println(f.normalizedNumberOfMovesPlayedSofar));
//        System.out.println("");



//        long correctN = dataSet.features.stream()
//            .filter(f -> f.correctAndNoMindChange).count();
//
//        long notCorrectN = dataSet.features.stream()
//            .filter(f -> !f.correctAndNoMindChange).count();

   //     System.out.println("correct: " + correctN + ", not correct: " + notCorrectN);

//if (notCorrectN > 0 ) {
//    double entropy = dataSet.features.stream()
//        .filter(f -> !f.correctAndNoMindChange)
//        .min(Comparator.comparing(SelfCriticalLabeledFeature::getEntropy))
//        .get().getEntropy();
//
//
//    System.out.println("below this entropy only correct guesses: " + entropy);
//
//
//        long countReliableDataPoints = dataSet.features.stream()
//            .filter(f -> f.entropy < entropy).count();
//
//        long countAllDataPoints = dataSet.features.stream()
//            .count();
//
//        System.out.println("reliable data points " + countReliableDataPoints + " out of " + countAllDataPoints);
//}

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




//         dataSet = getSelfCriticalDataSet(numOfGames-7, numOfGames-7);
//          Collections.reverse(dataSet.features);
//
       List<Integer>  testResult = test.run(dataSet);
        int i = 42;
      //  testResult.stream().forEach(f -> System.out.println(f.intValue()));

        System.out.println("results not 0: " + testResult.stream().filter(r -> r != 0).count());
//        System.out.println("");
//        long numberOK = testResult.stream().filter(f -> f.booleanValue()).count();
//        long numberNOK = testResult.stream().filter(f -> !f.booleanValue()).count();
//        log.info("ok: " + numberOK + ", nok: " + numberNOK);
    }

    @NotNull
    private SelfCriticalDataSet getSelfCriticalDataSet(int firstGame, int lastGame) {

        SelfCriticalDataSet dataSet = new SelfCriticalDataSet();

        // int g = replayBuffer.getBuffer().getGames().size() - 7;

        int maxFullMoveOfAllGames = 0;
        for(int g = firstGame; g <= lastGame; g++) {
            Game game = replayBuffer.getBuffer().getGames().get(g);
            SelfCriticalGame scGame = new SelfCriticalGame();
            dataSet.data.add(scGame);
            boolean trusted = true;
            int totalMoves = game.getGameDTO().getActions().size();

            int maxFullMoveOfCurrentGame = totalMoves / 2;
            if (totalMoves % 2 != 0) {
                maxFullMoveOfCurrentGame++;
            }
            maxFullMoveOfAllGames = Math.max(maxFullMoveOfAllGames, maxFullMoveOfCurrentGame);
            for(int a = totalMoves-1; a >= 0; a--) {
                int fullMove = a/2;

                OneOfTwoPlayer toPlay = (a % 2 == 0) ?  OneOfTwoPlayer.PLAYER_A : OneOfTwoPlayer.PLAYER_B;
                SelfCriticalPosition pos = SelfCriticalPosition.builder()
                    .player(toPlay)
                    .fullMove(fullMove)
                    .build();
                SelfCriticalLabeledFeature feature = SelfCriticalLabeledFeature.builder()
                    .winner(((GoGame)game).whoWonTheGame().get() )
                    .value(game.getGameDTO().getRootValuesFromInitialInference().get(a))
                    .toPlay(toPlay)
                    .build();
                feature.transformRawToPreNormalizedInput();
                if (trusted && !feature.correct) {
                    trusted = false;
                    scGame.firstReliableFullMove = fullMove + 1;
                }
                //feature.setCorrectAndNoMindChange(trusted && feature.correct);

                scGame.normalizedEntropyValues.put(pos, (float)feature.getEntropy());
            }
            if (scGame.firstReliableFullMove == totalMoves/2)  {
                scGame.firstReliableFullMove = maxFullMoveOfAllGames;
            }
        }
        dataSet.maxFullMoves = maxFullMoveOfAllGames;
        return dataSet;
    }

}
