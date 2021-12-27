package ai.enpasos.muzero.go.debug;

import ai.enpasos.muzero.platform.agent.Inference;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.config.DeviceType;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Component
@Slf4j
public class GoArena {

    @Autowired
    MuZeroConfig config;

    @Autowired
    Inference inference;

    String dirNetwork1;
    String dirNetwork2;

    public void run() {

        dirNetwork1 = "./games/go/arena/network1/";
        dirNetwork2 = "./games/go/arena/network2/";

        int numGames = 10;
        List<Float> outcomesA = new ArrayList<>();
        IntStream.range(1, numGames).forEach(i -> outcomesA.add(playAGame(dirNetwork1)));
        log.info("1 starting ... " + outcomesA);
        List<Float> outcomesB = new ArrayList<>();
        IntStream.range(1, numGames).forEach(i -> outcomesB.add(playAGame(dirNetwork2)));
        log.info("2 starting ... " + outcomesB);
        List<Float> outcomes = new ArrayList<>();
        outcomes.addAll(outcomesA);
        outcomes.addAll(outcomesB);
        double fractionPlayer1wins = ((double) outcomes.stream()
                .filter(i -> i == 1.0f)
                .count())
                / outcomes.size();
        log.info("fractionPlayer1wins: " + fractionPlayer1wins);
    }


    // 1f player1 wins, -1f player2 wins - no draw here
    private float playAGame(String startingPlayer) {
        Game game = config.newGame();
        String currentPlayer = startingPlayer;
        while (!game.terminal()) {
            move(game, currentPlayer);
            currentPlayer = changePlayer(currentPlayer);
        }
        currentPlayer = changePlayer(currentPlayer);
        return (currentPlayer.equals(dirNetwork1) ? 1f : -1f) * game.getLastReward();
    }

    private void move(Game game, String player) {
        game.apply(inference.aiDecision(game.getGameDTO().getActions(), true, player, DeviceType.GPU));
    }

    private String changePlayer(String currentPlayer) {
        return currentPlayer.equals(dirNetwork1) ? dirNetwork2 : dirNetwork1;
    }

}
