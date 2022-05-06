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
import java.util.stream.IntStream;

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
        List<Game> games = allGames.subList(allGames.size()-n, allGames.size());
        List<Game> gameSeeds = new ArrayList<>();

        games.stream().forEach(game -> {
            surpriseEvaluation(game);
        });

        List<Game> gamesSorted = new ArrayList<>();
        gamesSorted.addAll(games);
        gamesSorted.sort(Comparator.comparing(Game::getSurpriseMax).reversed());

        gamesSorted.stream().forEach(game -> {
            surpriseSeedsPerGame(game, gameSeeds, n);
        });



        log.info("surpriseHandling, no of gameSeeds: " + gameSeeds.size());
        selfPlay.replayGamesToEliminateSurprise(network, gameSeeds);

    }
    private void surpriseEvaluation(Game game) {
        if (game.getGameDTO().getSurprises().size() == 0) return;

        double surprise[] = game.getGameDTO().getSurprises().stream().mapToDouble(eF -> (double)eF).toArray();

        double surpriseMean = Arrays.stream(surprise).average().orElseThrow(MuZeroException::new);
        double surpriseMax = Arrays.stream(surprise).max().orElseThrow(MuZeroException::new);

        game.setSurpriseMax(surpriseMax);
        game.setSurpriseMean(surpriseMean);


    }

    private void surpriseSeedsPerGame(Game game, List<Game> gameSeeds, int maxNumber) {
//        if (game.getGameDTO().getSurprises().size() == 0) return;
//        if (gameSeeds.size() > maxNumber) return;
//
//        double surprises[] =  game.getGameDTO().getSurprises();
//
//        boolean[] unexpectedSurpriseMarker = new boolean[surprises.length];
//
//        boolean firstCluster = false;
//        for(int i = surprises.length-1; i >= 0; i--)  {
//            // "3 sigma"
//            unexpectedSurpriseMarker[i] = surprises[i] > 9*game.getSurpriseMax();
//            // TODO configurable ... only one cluster is useful if there is only a reward source at the end of the game
//            // more generally there can me many clusters relevant
//            if (firstCluster && !unexpectedSurpriseMarker[i]) break;
//            if (unexpectedSurpriseMarker[i]) {
//                firstCluster = true;
//            }
//        }
//
//        // start new game branch for each surprise marker - 2
//        IntStream.range(0, surprises.length).forEach(i -> {
//            if (unexpectedSurpriseMarker[i]) {
//                Game newGame = game.copy(i);
//                newGame.setTTrainingStart(i);
//                gameSeeds.add(newGame);
//            }
//        });
//        game.setSurprises(null);

    }


}
