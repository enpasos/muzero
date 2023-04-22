package ai.enpasos.muzero.platform.agent.b_planning.service;

import ai.enpasos.muzero.platform.agent.d_experience.Game;
import ai.enpasos.muzero.platform.agent.b_planning.PlayParameters;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EpisodeRunner {

    @Autowired
    MuZeroConfig config;


    @Autowired
    SelfPlayGame selfPlayer;



    @Async()
    CompletableFuture<Game> playGame(PlayParameters playParameters, Game game) {
        log.trace("playGame");
        selfPlayer.play(game, playParameters);
        return CompletableFuture.completedFuture(game);
    }

}
