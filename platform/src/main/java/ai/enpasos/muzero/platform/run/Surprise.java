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
import java.util.ArrayList;
import java.util.Collections;
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
        List<Game> games = getRelevantGames(1000);
        getGamesWithSurprisesAboveThresholdBackInTime(games, 1000, 1000).getLeft();


    }


    public void handleOldSurprises(Network network) {
        List<Game> allGames = replayBuffer.getBuffer().getGames();

        List<Game> gamesWithOldSurprise = allGames.stream().filter(game -> game.getGameDTO().isSurprised()).collect(Collectors.toList());

        List<Game> gameSeeds = new ArrayList<>();


        gamesWithOldSurprise.forEach(game -> {
            Game newGame = game.copy((int) game.getGameDTO().getTStateA());
            newGame.getGameDTO().setTStateB(newGame.getGameDTO().getTStateA());
            newGame.getGameDTO().setTSurprise(0);
            newGame.getGameDTO().setSurprised(false);
            gameSeeds.add(newGame);
        });
        replayBuffer.removeGames(gamesWithOldSurprise);

        selfPlay.replayGamesToEliminateSurprise(network, gameSeeds);

    }

    public void markSurprise(int backInTime) {
        List<Game> games = this.replayBuffer.getBuffer().getGames();
        double quantil = this.getSurpriseThreshold(games);
        getGamesWithSurprisesAboveThresholdBackInTime(games, quantil, backInTime).getLeft();
    }

    public void markSurprise() {
        int n = config.getNumEpisodes() * config.getNumParallelGamesPlayed();
        List<Game> games = getRelevantGames(n);
        double quantil = this.getSurpriseThreshold(games);
        getGamesWithSurprisesAboveThresholdBackInTime(games, quantil, 1000).getLeft();
    }


    private List<Game> getRelevantGames(int numGames) {
        List<Game> games = replayBuffer.getBuffer().getGames().stream()
            .filter(game -> !game.getGameDTO().getSurprises().isEmpty())
            .collect(Collectors.toList());

        return games.subList(Math.max(games.size() - numGames, 0), games.size());


    }


    public double getSurpriseThreshold(List<Game> games) {
        List<Double> relevantSurprises = new ArrayList<>();
        games.forEach(g -> {
            int t = g.getGameDTO().getActions().size() - 1;
            IntStream.range(0, t).forEach(i -> relevantSurprises.add(Double.valueOf(g.getGameDTO().getSurprises().get(i))));
        });
        Collections.sort(relevantSurprises);
        Collections.reverse(relevantSurprises);

        double surpriseMean = relevantSurprises.stream().mapToDouble(x -> x).average().orElseThrow(MuZeroException::new);
        double surpriseMax = relevantSurprises.stream().mapToDouble(x -> x).max().orElseThrow(MuZeroException::new);

        double variance = relevantSurprises.stream().mapToDouble(x -> {
            var delta = x - surpriseMean;
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


    public List<Game> getGamesWithSurprisesAboveThreshold(List<Game> games, double surpriseThreshold) {


        games.forEach(game -> {
            GameDTO dto = game.getGameDTO();
            dto.setSurprised(false);
            dto.setTStateA(0);
            dto.setTStateB(0);
            dto.setTSurprise(0);
        });

        List<Game> gamesToInvestigate = games.stream()
            .filter(g -> g.getGameDTO().getSurprises().stream().mapToDouble(x -> x).max().getAsDouble() >= surpriseThreshold).collect(Collectors.toList());


        log.info("no of total games with surprise above threshold: " + gamesToInvestigate.size() + " / " + games.size());


        gamesToInvestigate.forEach(game -> {
            for (int t = game.getGameDTO().getSurprises().size() - 1; t >= 0; t--) {
                GameDTO dto = game.getGameDTO();
                double s = dto.getSurprises().get(t);
                if (s >= surpriseThreshold) {
                    dto.setTSurprise(t);
                    dto.setSurprised(true);
                    break;
                }
            }
        });
        return gamesToInvestigate;
    }

    //
    public Pair<List<Game>, List<Game>> getGamesWithSurprisesAboveThresholdBackInTime(List<Game> games, double surpriseThreshold, int backInTime) {


        games.forEach(game -> {
            GameDTO dto = game.getGameDTO();
            dto.setSurprised(false);
            dto.setTStateA(0);
            dto.setTStateB(0);
            dto.setTSurprise(0);
        });

        List<Game> gamesToInvestigate = games.stream()
            .filter(g -> g.getGameDTO().getSurprises().stream().mapToDouble(x -> x).max().getAsDouble() >= surpriseThreshold).collect(Collectors.toList());


        log.info("no of total games with surprise above threshold: " + gamesToInvestigate.size() + " / " + games.size());

        List<Game> gamesToInvestigateHere = new ArrayList<>();

        gamesToInvestigate.forEach(game -> {
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
        log.info("no of games with surprise above threshold for backInTime=" + backInTime + ": " + gamesToInvestigateHere.size() + " / " + games.size());
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

    public void measureValueAndSurprise(Network network, List<Game> games) {
        games.forEach(Game::beforeReplayWithoutChangingActionHistory);
        mearsureValueAndSurpriseMain(network, games);
        games.forEach(Game::afterReplay);
    }

    private void mearsureValueAndSurpriseMain(Network network, List<Game> games) {
        List<List<Game>> gameBatches = ListUtils.partition(games, config.getNumParallelGamesPlayed());
        List<Game> resultGames = new ArrayList<>();
        int i = 1;
        for (List<Game> gameList : gameBatches) {
            log.info("justReplayGamesWithInitialInference " + i++ + " of " + gameBatches.size());
            resultGames.addAll(selfPlay.justReplayGamesWithInitialInference(network, gameList));
        }

    }

    public void measureValueAndSurprise(Network network, List<Game> games, int backInTime) {
        games.forEach(game -> game.beforeReplayWithoutChangingActionHistory(backInTime));
        mearsureValueAndSurpriseMain(network, games);
        games.forEach(Game::afterReplay);
    }


}
