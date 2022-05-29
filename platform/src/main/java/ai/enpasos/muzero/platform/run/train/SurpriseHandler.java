package ai.enpasos.muzero.platform.run.train;

import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class SurpriseHandler {


    @Autowired
    MuZeroConfig config;


    @Autowired
    ReplayBuffer replayBuffer;


    @Autowired
    SelfPlay selfPlay;


    public void surpriseHandling(Network network) {
        int n = config.getNumEpisodes() * config.getNumParallelGamesPlayed();
        List<Game> allGames = replayBuffer.getBuffer().getGames();
        List<Game> games = allGames.subList(allGames.size() - n, allGames.size());
        List<Game> gameSeeds = new ArrayList<>();

        games.forEach(this::surpriseEvaluation);

        List<Game> gamesSorted = new ArrayList<>(games);
        gamesSorted.sort(Comparator.comparing(Game::getSurpriseMax).reversed());

        gamesSorted.forEach(game -> surpriseSeedsPerGame(game, gameSeeds, n));


        log.info("surpriseHandling, no of gameSeeds: " + gameSeeds.size());
        selfPlay.replayGamesToEliminateSurprise(network, gameSeeds);

    }

    private void surpriseEvaluation(Game game) {
        if (game.getGameDTO().getSurprises().isEmpty()) return;

        double[] surprise = game.getGameDTO().getSurprises().stream().mapToDouble(eF -> (double) eF).toArray();

        double surpriseMean = Arrays.stream(surprise).average().orElseThrow(MuZeroException::new);
        double surpriseMax = Arrays.stream(surprise).max().orElseThrow(MuZeroException::new);

        game.setSurpriseMax(surpriseMax);
        game.setSurpriseMean(surpriseMean);


    }

    private void surpriseSeedsPerGame(Game game, List<Game> gameSeeds, int maxNumber) {

    }


}
