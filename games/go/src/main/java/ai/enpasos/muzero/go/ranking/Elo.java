package ai.enpasos.muzero.go.ranking;

import ai.enpasos.muzero.platform.common.MuZeroException;

public class Elo {

    /**
     *  (from https://europeangodatabase.eu/EGD/EGF_rating_system.php)
     *
     *  The rating algorithm was updated starting 2021. The whole database from back in 1996 was recalculated with this algorithm. You can find the old algorithm here.
     *
     * Ratings are updated by: r' = r + con * (Sa - Se) + bonus
     * r is the old EGD rating (GoR) of the player
     * r' is the new EGD rating of the player
     * Sa is the actual game result (1.0 = win, 0.5 = jigo, 0.0 = loss)
     * Se is the expected game result as a winning probability (1.0 = 100%, 0.5 = 50%, 0.0 = 0%). See further below for its computation.
     * con is a factor that determines rating volatility (similar to K in regular Elo rating systems): con = ((3300 - r) / 200)^1.6
     * bonus (not found in regular Elo rating systems) is a term included to counter rating deflation: bonus = ln(1 + exp((2300 - rating) / 80)) / 5
     *
     * Se is computed by the Bradley-Terry formula: Se = 1 / (1 + exp(β(r2) - β(r1)))
     * r1 is the EGD rating of the player
     * r2 is the EGD rating of the opponent
     * β is a mapping function for EGD ratings: β = -7 * ln(3300 - r)
     */
    public static int calculateNewElo(int oldRPlayer, int oldROpponent, double averagePointsOfPlayer) {
        if (oldRPlayer < -3300 || oldROpponent < -3300) throw new MuZeroException("elo is expected to be -3300 or above");
        if (oldRPlayer == -3300) return -3300;

        double r = oldRPlayer;
        double r2 = oldROpponent;
        double sa = averagePointsOfPlayer;
        double con = Math.pow((3300d - r) / 200d, 1.6d);
        double se;
        if (oldROpponent == -3300) {
            se = 1d;
        } else {
            se = 1d / (1d + Math.exp(beta(r2) - beta(r)));
        }
        double bonus = Math.log(1d + Math.exp((2300d - r) / 80d)) / 5d;
        int elo =  (int) Math.round(r + con * (sa - se) + bonus);
        if (elo < -3300) {
            elo = -3300;
        }
        return elo;
    }

    public static double beta(double r) {
        return -7d * Math.log(3300d - r);
    }
}
