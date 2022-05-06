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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
        double quantil = this.getQuantil(games, 1000);
        List<Game> gamesToInvestigate = getGamesWithSurprisesAboveQuantil( games,  quantil);



//        NumberFormat nf = NumberFormat.getInstance();
//        nf.setMinimumFractionDigits(6);
//        nf.setMaximumFractionDigits(6);
//        Game game = gamesToInvestigate.get(gamesToInvestigate.size() - 1);
//        log.info("a game's surprises: ");
//        log.info("" + nf.format(game.getTSurprise()));

    }


    public void handleOldSurprises(Network network) {

        List<Game> allGames = replayBuffer.getBuffer().getGames();

        List<Game> gamesWithOldSurprise = allGames.stream().filter(game -> game.getGameDTO().isSurprised()).collect(Collectors.toList());

        List<Game> gameSeeds = new ArrayList<>();

        gamesWithOldSurprise.forEach(game -> {
            Game newGame = game.copy((int)game.getGameDTO().getTStateA());
            newGame.getGameDTO().setTStateB(newGame.getGameDTO().getTStateA());
            newGame.getGameDTO().setTSurprise(0);
            newGame.getGameDTO().setSurprised(false);
            gameSeeds.add(newGame);
        });
        replayBuffer.removeGames(gamesWithOldSurprise);

        selfPlay.replayGamesToEliminateSurprise(network, gameSeeds);

    }



    public void markSurprise() {
        int n = config.getNumEpisodes() * config.getNumParallelGamesPlayed();
        List<Game> games =  getRelevantGames(n);
        double quantil = this.getQuantil(games, n);
        List<Game> gamesToInvestigate = getGamesWithSurprisesAboveQuantil( games,  quantil);
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

    private double getQuantil(List<Game> games, int numHighestValues) {


        double surpriseMean = games.stream().mapToDouble(g -> g.getGameDTO().getSurprises().stream().mapToDouble(x -> x).average().getAsDouble())
            .average().getAsDouble();
        double surpriseMax = games.stream().mapToDouble(g -> g.getGameDTO().getSurprises().stream().mapToDouble(x -> x).max().getAsDouble())
            .max().getAsDouble();
        List<Double> allValues = new ArrayList<>();
        games.stream().forEach(g -> g.getGameDTO().getSurprises().stream().mapToDouble(x -> x).forEach(allValues::add));
        Collections.sort(allValues);
        Collections.reverse(allValues);

        double quantil = allValues.get(numHighestValues);

        log.info("surprisesMean: " + surpriseMean);
        log.info("surprisesMax: " + surpriseMax);
        log.info("surprisesQuantil: " + quantil);

        return quantil;
    }
    private List<Game> getGamesWithSurprisesAboveQuantil(List<Game> games, double quantil) {


        List<Game> gamesToInvestigate = games.stream()
            .filter(g -> g.getGameDTO().getSurprises().stream().mapToDouble(x -> x).max().getAsDouble() >= quantil).collect(Collectors.toList());


        gamesToInvestigate.stream().forEach(game -> {
            for (int t = game.getGameDTO().getSurprises().size() - 1; t >= 0; t--) {
                GameDTO dto = game.getGameDTO();
                double s = dto.getSurprises().get(t);
                if (s >= quantil) {
                    dto.setTSurprise(t);
                    dto.setTStateA(Math.max(0,t-3));
                    dto.setTStateB(Math.min(game.getGameDTO().getSurprises().size()-1, t+1));
                    dto.setSurprised(true);
                    break;
                }
            }
        });
        return gamesToInvestigate;
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
