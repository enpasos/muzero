package ai.enpasos.muzero.platform.agent.b_planning.service;

import ai.enpasos.muzero.platform.agent.d_experience.Game;
import ai.enpasos.muzero.platform.agent.d_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.b_planning.PlayParameters;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static ai.enpasos.muzero.platform.config.PlayTypeKey.HYBRID;
import static ai.enpasos.muzero.platform.config.PlayTypeKey.REANALYSE;

@Component
@Slf4j
public class PlayService {

    @Autowired
    EpisodeRunner episodeRunner;



    @Autowired
    GameBuffer gameBuffer;


    @Autowired
    MuZeroConfig config;

    public List<Game> playNewGames( int numGames, PlayParameters playParameters) {
        List<Game> games = new ArrayList<>();

        for (int i = 0; i < numGames; i++) {
            Game game = config.newGame();
            games.add(game);
        }
        games.stream().forEach(game -> {
            game.getGameDTO().setTdSteps(config.getTdSteps());
            game.setPlayTypeKey(this.config.getPlayTypeKey());
        });
        if (config.getTrainingTypeKey() == HYBRID) {
            hybridConfiguration(games);
        }
        if (config.getPlayTypeKey() == REANALYSE) {
            reanalyseConfiguration(games);
        }
        return playGames( games, playParameters);
    }

    private void hybridConfiguration(List<Game> games) {
        int gameLength = gameBuffer.getAverageGameLength();
        hybridConfiguration(games, gameLength);
    }

    private void hybridConfiguration(List<Game> games, int gameLength) {
        games.stream().forEach(game -> {
            game.getGameDTO().setHybrid(true);
            if (game.getGameDTO().getTHybrid() == -1) {
                game.getGameDTO().setTHybrid(ThreadLocalRandom.current().nextInt(0, gameLength + 1));
            }
        });
    }
    private void reanalyseConfiguration(List<Game> games) {
        games.forEach(game -> {
            game.setOriginalGameDTO(game.getGameDTO().copy());
            game.getGameDTO().getPolicyTargets().clear();
            game.getGameDTO().setRootValueTargets(new ArrayList<>());
            game.getGameDTO().setEntropies(new ArrayList<>());
            game.getGameDTO().setMaxEntropies(new ArrayList<>());
            game.getGameDTO().setRootValuesFromInitialInference(new ArrayList<>());
            game.getGameDTO().setActions(new ArrayList<>());
            game.getGameDTO().setRewards(new ArrayList<>());
            game.replayToPosition(0);
        });
    }




    public List<Game> playGames(  List<Game> games, PlayParameters playParameters) {

        giveOneOfTheGamesADebugFlag(games);
        int averageGameLength = gameBuffer.getAverageGameLength();
        playParameters.setPRandomActionRawAverage(this.gameBuffer.getPRandomActionRawAverage());
        playParameters.setAverageGameLength(averageGameLength);

        CompletableFuture<Game>[] futures = games.stream().map(g ->
            episodeRunner.playGame( playParameters, g)
        ).toArray(CompletableFuture[]::new);

        // wait for all futures to complete
        CompletableFuture.allOf(futures).join();


        // collect games from futures
        List<Game> gamesReturn = new ArrayList<>();
        for (CompletableFuture<Game> future : futures) {
            try {
                gamesReturn.add(future.get());
            } catch (InterruptedException e) {
                log.warn("Interrupted!", e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new MuZeroException(e);
            }
        }

        return gamesReturn;
    }

    private void giveOneOfTheGamesADebugFlag(List<Game> games) {
        games.stream().forEach(game -> game.setDebug(false));
        if (!games.isEmpty()) {
            games.get(0).setDebug(true);
        }
    }
}
