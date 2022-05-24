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

import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.GameDTO;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class Surprise {
    @Autowired
    MuZeroConfig config;



    @Autowired
    ReplayBuffer replayBuffer;

    @Autowired
    SelfPlay selfPlay;


    @SuppressWarnings("squid:S3740")
    public void run() {
        replayBuffer.loadLatestState();
        List<Game> games =  getRelevantGames(1000);
        double quantil = this.getSurpriseThreshold(games);
        List<Game> gamesToInvestigate = getGamesWithSurprisesAboveThreshold( games,  1000, 1000).getLeft();



//        NumberFormat nf = NumberFormat.getInstance();
//        nf.setMinimumFractionDigits(6);
//        nf.setMaximumFractionDigits(6);
//        Game game = gamesToInvestigate.get(gamesToInvestigate.size() - 1);
//        log.info("a game's surprises: ");
//        log.info("" + nf.format(game.getTSurprise()));

    }


    public void handleOldSurprises(Network network) {
        int sizeBefore = replayBuffer.getBuffer().getData().size();
        List<Game> allGames = replayBuffer.getBuffer().getGames();

        List<Game> gamesWithOldSurprise = allGames.stream().filter(game -> game.getGameDTO().isSurprised()).collect(Collectors.toList());

        List<Game> gameSeeds = new ArrayList<>();

       // Set<Game> gameSeedsSet = new HashSet<>();

        gamesWithOldSurprise.forEach(game -> {
            Game newGame = game.copy((int)game.getGameDTO().getTStateA());
            newGame.getGameDTO().setTStateB(newGame.getGameDTO().getTStateA());
            newGame.getGameDTO().setTSurprise(0);
            newGame.getGameDTO().setSurprised(false);
            gameSeeds.add(newGame);
          //  gameSeedsSet.add(newGame);
        });
   //     log.info("gameSeedsList size: " + gameSeeds.size());
      //  log.info("gameSeedsSet size: " + gameSeedsSet.size());
        replayBuffer.removeGames(gamesWithOldSurprise);
  //      log.info("replayBuffer size (after replayBuffer.removeGames): " + replayBuffer.getBuffer().getData().size());

        selfPlay.replayGamesToEliminateSurprise(network, gameSeeds);
        int sizeAfter = replayBuffer.getBuffer().getData().size();
//        if (sizeBefore != sizeAfter) {
//            String message = "buffer size should not be changed by handleOldSurprises, but it changes from " + sizeBefore + " to " + sizeAfter;
//            log.error(message);
//            throw new MuZeroException(message);
//        }
    }

    public void markSurprise(int backInTime) {
        //int n = config.getNumEpisodes() * config.getNumParallelGamesPlayed();
        List<Game> games = this.replayBuffer.getBuffer().getGames();
        double quantil = this.getSurpriseThreshold(games);
        List<Game> gamesToInvestigate = getGamesWithSurprisesAboveThreshold( games,  quantil, backInTime).getLeft();
    }

    public void markSurprise() {
        int n = config.getNumEpisodes() * config.getNumParallelGamesPlayed();
        List<Game> games =  getRelevantGames(n);
        double quantil = this.getSurpriseThreshold(games);
        List<Game> gamesToInvestigate = getGamesWithSurprisesAboveThreshold( games,  quantil, 1000).getLeft();
    }



//    private List<Game> getGamesToInvestigate(int numGames, double fraction) {
//        return getGamesToInvestigate(null, numGames, fraction);
//    }


    private List<Game> getRelevantGames(int numGames) {
       // int bufferSize = replayBuffer.getBuffer().getGames().size();
        List<Game> games = replayBuffer.getBuffer().getGames().stream()
            .filter(game -> !game.getGameDTO().getSurprises().isEmpty())
            .collect(Collectors.toList());

        return games.subList(Math.max(games.size() - numGames, 0), games.size());


    }


    public double getSurpriseThreshold(List<Game> games ) {
         List<Double> relevantSurprises = new ArrayList<>();
        games.stream().forEach(g -> {
            int t = g.getGameDTO().getActions().size() - 1;
            IntStream.range(0, t).forEach(i -> relevantSurprises.add(Double.valueOf(g.getGameDTO().getSurprises().get(i))));
        });
//         games.stream().forEach(g -> {
//            int t = g.getGameDTO().getActions().size() - 1;
//            IntStream.range(t-backInTime, t).forEach(i -> relevantSurprises.add(Double.valueOf(g.getGameDTO().getSurprises().get(i))));
//        });
        Collections.sort(relevantSurprises);
        Collections.reverse(relevantSurprises);

        double surpriseMean = relevantSurprises.stream().mapToDouble(x -> x).average().orElseThrow(MuZeroException::new);
        double surpriseMax = relevantSurprises.stream().mapToDouble(x -> x).max().orElseThrow(MuZeroException::new);

        double variance =  relevantSurprises.stream().mapToDouble(x -> {
              var delta = x-surpriseMean;
              return delta * delta;
          }).sum() / relevantSurprises.size();

        double stddev = Math.sqrt(variance);
        double surpriseThreshold = surpriseMean + 3 * stddev;

        log.info("surprisesCount: " + relevantSurprises.size());
        log.info("surprisesMean: " + surpriseMean);
        log.info("surprisesThreshold: " + surpriseThreshold);
        log.info("surprisesMax: " + surpriseMax);

        return surpriseThreshold;
    }






//
   public Pair<List<Game>, List<Game>> getGamesWithSurprisesAboveThreshold(List<Game> games, double surpriseThreshold, int backInTime) {


        games.stream().forEach(game -> {
               GameDTO dto = game.getGameDTO();
               dto.setSurprised(false);
                dto.setTStateA(0);
                dto.setTStateB(0);
                dto.setTSurprise(0);
        });

        List<Game> gamesToInvestigate = games.stream()
            .filter(g -> g.getGameDTO().getSurprises().stream().mapToDouble(x -> x).max().getAsDouble() >= surpriseThreshold).collect(Collectors.toList());


       gamesToInvestigate.stream().forEach(game -> {
           GameDTO dto = game.getGameDTO();

       });
       log.info("no of total games with surprise above threshold: " + gamesToInvestigate.size());

       List<Game> gamesToInvestigateHere = new ArrayList<>();

       gamesToInvestigate.stream().forEach(game -> {
            for (int t = game.getGameDTO().getSurprises().size() - 1; t >= game.getGameDTO().getSurprises().size() - 1 - backInTime; t--) {
                GameDTO dto = game.getGameDTO();
                double s = dto.getSurprises().get(t);
                if (s >= surpriseThreshold) {
                    dto.setTSurprise(t);
                    dto.setSurprised(true);
                    gamesToInvestigateHere.add(game);
                    break;
                }
            }
        });
        log.info("no of games with surprise above threshold for backInTime=" + backInTime + ": " + gamesToInvestigateHere.size());
        return Pair.of(gamesToInvestigateHere, gamesToInvestigate);
    }

    public String csvString(double[] surprises) {
        StringWriter stringWriter = new StringWriter();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(4);
        nf.setMaximumFractionDigits(4);
        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';').setHeader("t", "surprise").build())) {
            IntStream.range(0, surprises.length).forEach(
                i -> {
                    double s = surprises[i];
                    try {
                        csvPrinter.printRecord(i, nf.format(s));
                    } catch (Exception e) {
                        // ignore
                    }
                });

        } catch (IOException e) {
            throw new MuZeroException(e);
        }
        return stringWriter.toString();
    }

    public void measureValueAndSurprise(Network network, List<Game> games ) {
        games.stream().forEach(game -> game.beforeReplayWithoutChangingActionHistory( ));
        mearsureValueAndSurpriseMain(network, games);
    }

    private void mearsureValueAndSurpriseMain(Network network, List<Game> games) {
        List<List<Game>> gameBatches = ListUtils.partition(games, config.getNumParallelGamesPlayed());
        List<Game> resultGames = new ArrayList<>();
        int i = 1;
        for (List<Game> gameList : gameBatches) {
            log.info("justReplayGamesWithInitialInference " + i++ + " of "+ gameBatches.size());
            resultGames.addAll(selfPlay.justReplayGamesWithInitialInference(network, gameList));
        }
        games.stream().forEach(Game::afterReplay);
    }

    public void measureValueAndSurprise(Network network, List<Game> games, int backInTime) {
        games.stream().forEach(game -> game.beforeReplayWithoutChangingActionHistory(backInTime));
        mearsureValueAndSurpriseMain(network, games);
    }


//    public void train(Integer epoch, Network network) {
//        if (epoch < 40) return;
//        List<Game> gamesToInvestigate = getGamesToInvestigate(network, 1000, 0.001d);
//        this.replayBuffer.removeGames(gamesToInvestigate);
//
//        int numOfReplaysPerGame = 5;
//
//        List<Game> gamesToReplay = new ArrayList<>();
//
//        gamesToInvestigate.stream().forEach(game -> {
//            List<Integer> actions = game.actionHistory().getActionIndexList();
//            IntStream.range(0, numOfReplaysPerGame).forEach(i -> {
//                Game newGame = config.newGame();
//                int replayStart = game.getTSurprise() - 3 + i;
//                if (replayStart < 0) return;
//                newGame.setTTrainingStart(replayStart);
//                IntStream.range(0, replayStart).forEach(t -> newGame.apply(actions.get(t)));
//                if (!newGame.terminal()) {
//                    gamesToReplay.add(newGame);
//                }
//            });
//
//        });
//
//
//        selfPlay.replayGamesToEliminateSurprise(network, gamesToReplay);
//                /*
//           - remove the game from the memory
//           - replay the game a number of N times and add them to memory
//             - start from tSurprise, think deeper for 10 steps than back to normal until the end of the game
//           - mark the actions before the surprise in such a way that they are not taken for learning mor than one game
//
//         */
//
//
//    }
}
