package ai.enpasos.muzero.platform.agent.d_model.service;

import java.util.Comparator;
import java.util.stream.IntStream;

public class ZipperFunctions {


    /**
     * Determine the unroll steps for each game e starting from n[e] - k with n[e] = bOk[e].length.
     * n[e] - 1 - k < 0 then return -1
     * @param trainingNeeded  boolean[][][] bOk[e][t1][t2] = true if training is needed from t = t1 and to t = t2
     * @param k     the starting time of unrolling is t[e] = n[e] - 1 - k with n[e] = bOk[e].length
     * @return the unroll steps for each game e starting from n[e] - 1 - k with n[e] = bOk[e].length
     */
    public static int[] determineUnrollSteps(boolean[][][] trainingNeeded,  int k) {

        int[] us = new int[trainingNeeded.length];

        // iterating the episodes
        for (int e = 0; e < trainingNeeded.length; e++) {
            int n = trainingNeeded[e].length;
            if (n - k - 1 < 0) {
                us[e] = -1;
            } else {
                // anyone needs a training from starting time n - 1 - k to max time n - 1
                for (int i = 0; i <= k; i++) {
                    if (trainingNeeded[e][n - 1 - k][n - 1 - k + i]) {
                        us[e] = i;
                    }
                }
            }
        }
        return us;
    }

//    public static boolean zipperClosed(boolean[][] bOk, int from, int to) {
//        for (int i = from; i < to; i++) {
//            if (!bOk[i][to]) return false;
//        }
//        return true;
//    }


    public static boolean[][][] trainingNeeded(boolean[][][] bOk) {
        boolean[][][] trainingNeeded = new boolean[bOk.length][][];
        for (int e = 0; e < bOk.length; e++) {
            int n = bOk[e].length;
            trainingNeeded[e] = new boolean[n][n];
            for (int to = 0; to < n; to++) {
                boolean zipperClosed = true;
                for (int tau = 0; tau <= to; tau++) {
                    trainingNeeded[e][to - tau][to] = zipperClosed;
                    zipperClosed = bOk[e][to - tau][to];
                }
            }
        }

        return trainingNeeded;
    }

    /**
     * Sort the indices of the games by the unroll steps, but omit the games with unroll steps -1
     * @param us unroll steps for each game e starting from n[e] - 1 - k with n[e] = bOk[e].length
     * @return the indices of the games sorted by the unroll steps
     */
    static int[] sortedAndFilteredIndices(int[] us) {
        return IntStream.range(0, us.length).boxed().filter(i -> us[i]!=-1).sorted(Comparator.comparingInt(i -> us[i])).mapToInt(i -> i).toArray();
    }
}
