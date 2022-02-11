package ai.enpasos.muzero.go.run;

import ai.enpasos.muzero.go.ranking.BattleDTO;
import ai.enpasos.muzero.go.ranking.Ranking;
import ai.enpasos.muzero.go.ranking.RankingEntryDTO;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class GoElo {

    @Autowired
    MuZeroConfig config;


    @Autowired
    Ranking ranking;

    @Autowired
    GoArena goArena;

    public void run() {

//        ranking.loadRanking();
//        ranking.clear();


        ranking.assureAllPlayerInRankingList();

        int a = ranking.selectPlayerWithHighestEpoch();
        int b = ranking.selectPlayerWithLowestEpoch();


        int eloB = ranking.getElo(b);
        if (eloB == Integer.MIN_VALUE) {
            eloB = -3000;
            ranking.setElo(b, eloB);
        }

        int eloA = ranking.getElo(a);
        if (eloA == Integer.MIN_VALUE) {
            eloA = -3000;
            ranking.setElo(a, eloA);
        }

        int numGamesPerBattle = 1000;
        double resultPlayerA = battle(a, b, numGamesPerBattle);

        ranking.addBattle(a, b, resultPlayerA, numGamesPerBattle);


        ranking.saveRanking();
    }


    public double battle(int a, int b, int numGames) {
        return goArena.battleAndReturnAveragePointsFromPlayerAPerspective(numGames, a + "", b + "");
    }


}
