package ai.enpasos.muzero.go.run;

import ai.enpasos.muzero.go.ranking.Ranking;
import ai.enpasos.muzero.go.ranking.RankingListDTO;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

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
//        ranking.clear();

        var numOfBattles = 10;
        var numGamesPerBattle = 100;

        assureThereIsSomeRankingForAllNetworks(numGamesPerBattle);

        IntStream.range(0, numOfBattles).forEach(i -> {
            ranking.saveRanking();
            randomBattle(numGamesPerBattle);
        });

        printRankingList();

        ranking.saveRanking();

    }

    public void assureThereIsSomeRankingForAllNetworks() {
        assureThereIsSomeRankingForAllNetworks(0);
    }

    private void assureThereIsSomeRankingForAllNetworks(int numGamesPerBattle) {
        if (ranking.exists()) {
            ranking.loadRanking();
            ranking.assureAllPlayerInRankingList();
            battleLatestEpochAgainstHighestEpochWithElo(numGamesPerBattle);
        } else {
            ranking.assureAllPlayerInRankingList();
            firstRanking(numGamesPerBattle);
        }

        ranking.fillMissingRankingsByLinearInterpolation();
    }


    private void printRankingList( ) {
        RankingListDTO rankingList = ranking.getRankingList();
        rankingList.sortByEpoch();
        System.out.println("epoch;elo");
        rankingList.getRankings().stream().forEach(r ->
            System.out.println(r.getEpochPlayer()+ ";" + r.getElo())
        );
    }

    private void randomBattle(int numGamesPerBattle) {
        RankingListDTO rankingList = ranking.getRankingList();
        rankingList.sortByElo();

        int challengerIndex = ThreadLocalRandom.current().nextInt(0, rankingList.getRankings().size() - 1);
        int a = rankingList.getRankings().get(challengerIndex).getEpochPlayer();
        int b = rankingList.getRankings().get(challengerIndex+1).getEpochPlayer();

        double resultPlayerA = battle(a, b, numGamesPerBattle);
        ranking.addBattle(a, b, resultPlayerA, numGamesPerBattle);
    }

    private void battleLatestEpochAgainstHighestEpochWithElo(int numGamesPerBattle) {
        if (numGamesPerBattle < 1) return;
        int a = ranking.selectPlayerWithHighestEpoch();
        int b = ranking.selectPlayerWithHighestEpochThatHasRanking();
        if (a == b) return;

        if (ranking.getElo(a) == Integer.MIN_VALUE) {
            ranking.setElo(a, ranking.getElo(b));
        }

        double resultPlayerA = battle(a, b, numGamesPerBattle);
        ranking.addBattle(a, b, resultPlayerA, numGamesPerBattle);
    }

    private void firstRanking(int numGamesPerBattle) {

        int a = ranking.selectPlayerWithHighestEpoch();
        int eloA = ranking.getElo(a);
        if (eloA == Integer.MIN_VALUE) {
            eloA = -3000;
            ranking.setElo(a, eloA);
        }


        int b = ranking.selectPlayerWithLowestEpoch();
        int eloB = ranking.getElo(b);
        if (eloB == Integer.MIN_VALUE) {
            eloB = -3000;
            ranking.setElo(b, eloB);
        }

        battleLatestEpochAgainstHighestEpochWithElo(numGamesPerBattle);

    }


    public double battle(int a, int b, int numGames) {
        return goArena.battleAndReturnAveragePointsFromPlayerAPerspective(numGames, a + "", b + "");
    }


}
