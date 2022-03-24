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
            surpriseSeedsPerGame(game, gameSeeds);
        });

        log.info("surpriseHandling, no of gameSeeds: " + gameSeeds.size());
        selfPlay.replayGamesToEliminateSurprise(network, gameSeeds);

    }

    private void surpriseSeedsPerGame(Game game, List<Game> gameSeeds) {
        if (game.getGameDTO().getEntropies().size() == 0) return;
        double e[] = game.getGameDTO().getEntropies().stream().mapToDouble(eF -> (double)eF).toArray();
        double eMean[] =  new double[e.length];
        IntStream.range(1, e.length-1).forEach(i -> {
            eMean[i] = (e[i-1] + 2*e[i] + e[i+1])/4d;
        });
        eMean[0] = (2*e[0] + e[1])/3d;
        eMean[e.length-1] = (e[e.length-2] + 2*e[e.length-1])/3d;

        double eVarLocal[] =  new double[e.length];
        IntStream.range(0, e.length).forEach(i -> {
            double d = e[i] - eMean[i];
            eVarLocal[i] = d * d;
        });
        double eVar = Arrays.stream(eVarLocal).average().orElseThrow(MuZeroException::new);

        boolean[] unexpectedSurpriseMarker = new boolean[e.length];

        boolean firstCluster = false;
        for(int i = e.length-1; i >= 0; i--)  {
            // "3 sigma"
            unexpectedSurpriseMarker[i] = eVarLocal[i] > 9*eVar;
            // TODO configurable ... only one cluster is useful if there is only a reward source at the end of the game
            // more generally there can me many clusters relevant
            if (firstCluster && !unexpectedSurpriseMarker[i]) break;
            if (unexpectedSurpriseMarker[i]) {
                firstCluster = true;
            }
        }

        // start new game branch for each surprise marker - 2
        IntStream.range(0, e.length).forEach(i -> {
            if (unexpectedSurpriseMarker[i]) {
                Game newGame = game.copy(i);
                newGame.setTTrainingStart(i);
                gameSeeds.add(newGame);
            }
        });

    }


}
