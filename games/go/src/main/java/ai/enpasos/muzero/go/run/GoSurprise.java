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

import ai.enpasos.muzero.go.ranking.Ranking;
import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class GoSurprise {
    @Autowired
    MuZeroConfig config;

    @Autowired
    Inference inference;

    @Autowired
    GoElo goElo;


    @Autowired
    Ranking ranking;


    @Autowired
    ReplayBuffer replayBuffer;

    @Autowired
    SelfPlay selfPlay;


    @SuppressWarnings("squid:S3740")
    public void run() {
        replayBuffer.loadLatestState();
        //      replayBuffer.keepOnlyTheLatestGames(10);
        List<Game> gamesToInvestigate = getGamesToInvestigate(10, 0.1d);


        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(6);
        nf.setMaximumFractionDigits(6);
        Game game = gamesToInvestigate.get(gamesToInvestigate.size() - 1);
        log.info("a game's surprises: ");
        log.info("" + nf.format(game.getTSurprise()));
        //  System.out.println(csvString(game.getSurprises()));


        // getMoreExperience(gamesToInvestigate);

    }

    private List<Game> getGamesToInvestigate(int numGames, double fraction) {
        return getGamesToInvestigate(null, numGames, fraction);
    }

    private List<Game> getGamesToInvestigate(Network network, int numGames, double fraction) {

//        replayBuffer.loadLatestState();
//        replayBuffer.keepOnlyTheLatestGames(numGames);

        int bufferSize = replayBuffer.getBuffer().getGames().size();
        List<Game> games = replayBuffer.getBuffer().getGames().subList(Math.max(bufferSize - numGames, 0), bufferSize);
        enrichGamesWithSurprises(network, games);

        double surpriseMean = games.stream().mapToDouble(g -> Arrays.stream(g.getSurprises()).average().getAsDouble())
            .average().getAsDouble();
        double surpriseMax = games.stream().mapToDouble(g -> Arrays.stream(g.getSurprises()).max().getAsDouble())
            .max().getAsDouble();
        List<Double> allValues = new ArrayList<>();
        games.stream().forEach(g -> Arrays.stream(g.getSurprises()).forEach(v -> allValues.add(v)));
        Collections.sort(allValues);


        log.info("surprisesMean: " + surpriseMean);
        log.info("surprisesMax: " + surpriseMax);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(6);
        nf.setMaximumFractionDigits(6);
        //    allValues.stream().forEach(v -> System.out.println(nf.format(v)));
        //  log.info("last game's surprises: ");
        //  System.out.println(csvString(games.get(games.size() - 1).getSurprises()));

        // value of the numGames-th highest value
        double quantil = allValues.get((int) (allValues.size() * (1d - fraction)));

        List<Game> gamesToInvestigate = games.stream()
            .filter(g -> Arrays.stream(g.getSurprises()).max().getAsDouble() >= quantil).collect(Collectors.toList());


        gamesToInvestigate.stream().forEach(game -> {
            for (int t = game.getSurprises().length - 1; t >= 0; t--) {
                double s = game.getSurprises()[t];
                if (s >= quantil) {
                    game.setTSurprise(t);
                    break;
                }
            }
        });
        return gamesToInvestigate;
    }


    public void enrichGamesWithSurprises(Network network, List<Game> gameList) {
        int numOfGames = gameList.size();


        double maxEntropy = Math.log(2);

        List<Game> newGameList = IntStream.range(0, numOfGames).mapToObj(i -> config.newGame()).collect(Collectors.toList());

        List<List<Integer>> actionList = IntStream.range(0, numOfGames)
            .mapToObj(i -> gameList.get(i).actionHistory().getActionIndexList())
            .collect(Collectors.toList());

        IntStream.range(0, numOfGames).forEach(g -> {
            Game game = gameList.get(g);
            game.setDone(false);
            int length = actionList.get(g).size();
            game.setValues(new double[length]);
            game.setEntropies(new double[length]);
            game.setSurprises(new double[length]);
        });

        int t = 0;
        //   try {    // TODO ... workaround for now
        while (gameList.stream().filter(g -> !g.isDone()).count() > 0) {

            //   for (int t = 0; t < length; t++) {
            List<Game> gamesToEvaluate = new ArrayList<>();
            for (int g = 0; g < numOfGames; g++) {
                Game game = gameList.get(g);
                if (!game.isDone()) {
                    Game newGame = newGameList.get(g);
                    newGame.apply(actionList.get(g).get(t));
                    gamesToEvaluate.add(newGame);
                }
            }
            double[] vs = null;
            if (network == null) {
                vs = inference.aiValue(gamesToEvaluate);
            } else {
                vs = inference.aiValue(network, gamesToEvaluate);
            }
            int i = 0;
            for (int g = 0; g < numOfGames; g++) {
                Game game = gameList.get(g);
                if (!game.isDone()) {
                    double v = vs[i++];
                    if (config.getPlayerMode() == PlayerMode.TWO_PLAYERS) {
                        v *= Math.pow(-1, t);
                    }
                    v = (v + 1d) / 2d;
                    game.getValues()[t] = v;
                    game.getEntropies()[t] = -v * Math.log(v) - (1.0 - v) * Math.log(1.0 - v);
                    game.getEntropies()[t] /= maxEntropy;
                    if (t > 0) {
                        double d = game.getEntropies()[t] - game.getEntropies()[t - 1];
                        game.getSurprises()[t] = d * d;
                    }
                    if (t + 1 == actionList.get(g).size()) {
                        game.setDone(true);
                    }
                }
            }
            t++;
        }

        IntStream.range(0, numOfGames).forEach(g -> {
            Game game = gameList.get(g);
            game.setValues(null);
            game.setEntropies(null);

        });
    }


    public String csvString(double[] surprises) {
        StringWriter stringWriter = new StringWriter();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(4);
        nf.setMaximumFractionDigits(4);
        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.withDelimiter(';').withHeader("t", "surprise"))) {
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


    public void train(Integer epoch, Network network) {
        if (epoch < 40) return;
        List<Game> gamesToInvestigate = getGamesToInvestigate(network, 1000, 0.001d);
        this.replayBuffer.removeGames(gamesToInvestigate);

        int numOfReplaysPerGame = 5;

        List<Game> gamesToReplay = new ArrayList<>();

        gamesToInvestigate.stream().forEach(game -> {
            List<Integer> actions = game.actionHistory().getActionIndexList();
            IntStream.range(0, numOfReplaysPerGame).forEach(i -> {
                Game newGame = config.newGame();
                int replayStart = game.getTSurprise() - 3 + i;
                if (replayStart < 0) return;
                newGame.setTTrainingStart(replayStart);
                IntStream.range(0, replayStart).forEach(t -> {
                    newGame.apply(actions.get(t));
                });
                if (!newGame.terminal()) {
                    gamesToReplay.add(newGame);
                }
            });

        });


        selfPlay.replayGamesToEliminateSurprise(network,  gamesToReplay);
                /*
           - remove the game from the memory
           - replay the game a number of N times and add them to memory
             - start from tSurprise, think deeper for 10 steps than back to normal until the end of the game
           - mark the actions before the surprise in such a way that they are not taken for learning mor than one game

         */


    }
}
