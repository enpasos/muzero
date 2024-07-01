package ai.enpasos.muzero.platform.agent.b_episode;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EpisodeRunner {

    @Autowired
    MuZeroConfig config;


    @Autowired
    SelfPlayGame selfPlayer;






    @Async()
    public CompletableFuture<Game> playGame(PlayParameters playParameters, Game game) {
        log.trace("playGame");
        selfPlayer.play(game, playParameters);
        return CompletableFuture.completedFuture(game);

    }

    @Async()
    public CompletableFuture<Game> uOkAnalyseGame(Game game, int unrollSteps) {
        log.trace("uOkAnalyseGame, unrollSteps: " + unrollSteps);
        selfPlayer.uOkAnalyseGame(game, unrollSteps);
        return CompletableFuture.completedFuture(game);
    }

    @Async()
    public CompletableFuture<Game> uOkAnalyseGame(Game game, List<Long> timestepIds, int unrollSteps) {
        log.trace("uOkAnalyseGame, unrollSteps: {}, timestepIds: {}", unrollSteps, timestepIds);
        selfPlayer.uOkAnalyseGame(game,timestepIds, unrollSteps);
        return CompletableFuture.completedFuture(game);
    }

}
