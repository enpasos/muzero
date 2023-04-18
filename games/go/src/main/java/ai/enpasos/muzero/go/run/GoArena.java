package ai.enpasos.muzero.go.run;

import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
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


    public void run() {
        battleAndReturnAveragePointsFromPlayerAPerspective(10, "0", "1");
    }

    public double battleAndReturnAveragePointsFromPlayerAPerspective(int numGames, String playerA, String playerB) {

        double[] outcomesA = play(true, playerA, playerB, numGames / 2);
        double[] outcomesB = play(false, playerA, playerB, numGames / 2);

        double[] outcomes = ArrayUtils.addAll(outcomesA, outcomesB);

        double fractionPlayer1wins = ((double) Arrays.stream(outcomes)
            .filter(i -> i == 1.0d)
            .count())
            / (double) outcomes.length;
        log.info("fractionPlayer1wins: " + fractionPlayer1wins);

        return fractionPlayer1wins;
    }


    // 1f player1 wins, -1f player2 wins - no draw here
    // TODO review and test
    private double[] play(boolean startingPlayerA, String playerA, String playerB, int n) {
        List<Game> gameList = IntStream.range(0, n).mapToObj(i -> config.newGame(true,true)).collect(Collectors.toList());
        List<Game> runningGames = new ArrayList<>(gameList);
        String currentPlayer = startingPlayerA ? playerA : playerB;
        while (!runningGames.isEmpty()) {
            runningGames = runningGames.stream().filter(g -> !g.terminal()).collect(Collectors.toList());
            currentPlayer = changePlayer(currentPlayer, playerA, playerB);
        }
        return gameList.stream()
            .mapToDouble(game -> (startingPlayerA ? 1f : -1f)
                * (game.getGameDTO().getActions().size() % 2 == 0 ? -1f : 1f)
                * game.getReward())
            .toArray();
    }

//    private void move(List<Game> games, int epoch) {
//        int[] actionsSelectedByAI = inference.aiDecisionForGames(games, true, epoch);
//        for (int g = 0; g < games.size(); g++) {
//            games.get(g).apply(actionsSelectedByAI[g]);
//        }
//    }

    private String changePlayer(String currentPlayer, String playerA, String playerB) {
        return currentPlayer.equals(playerA) ? playerB : playerA;
    }

}
