package ai.enpasos.muzero.go.run;

import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import ai.enpasos.muzero.platform.agent.memory.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Slf4j
public class GoArena {

    @Autowired
    MuZeroConfig config;

    @Autowired
    Inference inference;
    static final String ARENA = "./games/go/arena/";
    static final String ARENA_NETWORK_1 = ARENA + "network1/";
    static final String ARENA_NETWORK_2 = ARENA + "network2/";

    public void run() {



       // ARENA_NETWORK_1 = "./games/go/arena/network1/";
        //ARENA_NETWORK_2 = "./games/go/arena/network2/";

        int numGames = 100;
      //  List<Float> outcomesA = new ArrayList<>();
        double[] outcomesA = play(ARENA_NETWORK_1, numGames);
        log.info("1 starting ... " + Arrays.toString(outcomesA));
        double[] outcomesB = play(ARENA_NETWORK_2, numGames);
        log.info("2 starting ... " + Arrays.toString(outcomesB));


        double[] outcomes = ArrayUtils.addAll(outcomesA, outcomesB);


        double fractionPlayer1wins = ((double)Arrays.stream(outcomes)
            .filter(i -> i == 1.0d)
            .count())
            / (double)outcomes.length;
        log.info("fractionPlayer1wins: " + fractionPlayer1wins);
    }


    // 1f player1 wins, -1f player2 wins - no draw here
    private double[] play(String startingPlayer, int n) {
        List<Game> gameList = IntStream.range(0, n).mapToObj(i -> config.newGame()).collect(Collectors.toList());
        // Game game = config.newGame();
        List<Game> runningGames = new ArrayList<>();
        runningGames.addAll(gameList);
        String currentPlayer = startingPlayer;
        while (!runningGames.isEmpty()) {
            move(runningGames, currentPlayer);
            runningGames = runningGames.stream().filter(g -> !g.terminal()).collect(Collectors.toList());
            currentPlayer = changePlayer(currentPlayer);
        }
        currentPlayer = changePlayer(currentPlayer);
        String currentPlayerFinal = currentPlayer;
        return gameList.stream()
            .mapToDouble(game -> (startingPlayer.equals(ARENA_NETWORK_1) ? 1f : -1f)
                * (game.actionHistory().getActionIndexList().size() % 2 == 0 ? -1f : 1f)
                * game.getLastReward())
            .toArray();
       // return (currentPlayer.equals(ARENA_NETWORK_1) ? 1f : -1f) * game.getLastReward();
    }

    private void move(List<Game> games, String player) {
        int[] actionsSelectedByAI = inference.aiDecisionForGames(games, true, player);
        for(int g = 0; g < games.size(); g++) {
            games.get(g).apply(actionsSelectedByAI[g]);
        }
    }

    private String changePlayer(String currentPlayer) {
        return currentPlayer.equals(ARENA_NETWORK_1) ? ARENA_NETWORK_2 : ARENA_NETWORK_1;
    }

}
