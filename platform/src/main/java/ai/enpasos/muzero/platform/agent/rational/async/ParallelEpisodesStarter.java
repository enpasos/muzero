package ai.enpasos.muzero.platform.agent.rational.async;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.GameBuffer;
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

@Component
@Slf4j
public class ParallelEpisodesStarter {

    @Autowired
    EpisodeRunner episodeRunner;

    @Autowired
    GlobalState globalState;

    @Autowired
    GameBuffer replayBuffer;


    @Autowired
    MuZeroConfig config;

    public List<Game> playNewGames( int numGames, PlayParameters playParameters) {
        List<Game> games = new ArrayList<>();

        for (int i = 0; i < numGames; i++) {
            Game game = config.newGame();
            games.add(game);
        }
        if (config.getTrainingTypeKey() == HYBRID) {
            hybridConfiguration(games);
        }
        return playGames( games, playParameters);
    }

    private void hybridConfiguration(List<Game> games) {
        int gameLength = replayBuffer.getAverageGameLength();
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




    public List<Game> playGames(  List<Game> games, PlayParameters playParameters) {


        giveOneOfTheGamesADebugFlag(games);
        int averageGameLength = replayBuffer.getAverageGameLength();
        playParameters.setPRandomActionRawAverage(this.replayBuffer.getPRandomActionRawAverage());
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
