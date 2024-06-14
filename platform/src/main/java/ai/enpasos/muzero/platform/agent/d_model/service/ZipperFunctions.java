package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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


    public static boolean[][][] b_OK_From_S_in_Games(List<Game> games) {
        List<EpisodeDO> episodeDOList = games.stream().map(Game::getEpisodeDO).collect(Collectors.toList());
        return b_OK_From_S_in_Episodes(episodeDOList);
    }



    public static boolean[][][] b_OK_From_S_in_Episodes(List<EpisodeDO> episodeDOList) {

        boolean[][][] b_OK = new boolean[episodeDOList.size() ][][];
        for (int e = 0; e < episodeDOList.size(); e++) {
            EpisodeDO episodeDO = episodeDOList.get(e);
            int tmax = episodeDO.getLastTimeWithAction() + 1;
            b_OK[e] = new boolean[tmax + 1][tmax + 1];
            for (int to = 0; to <= tmax; to++) {
                int s = episodeDO.getTimeStep(to).getS();
                for (int from = 0; from <= to; from++) {
                    b_OK[e][from][to] = (to-from) < s;
                }
            }
        }
        return b_OK;
    }


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

    public static float[][][] trainingNeededFloat(boolean[][][] bOk, float forceScaleOnClosedZipper, boolean alwaysTrainTau1) {
        float[][][] trainingNeeded = new float[bOk.length][][];
        for (int e = 0; e < bOk.length; e++) {
            int n = bOk[e].length;
            trainingNeeded[e] = new float[n][n];
            for (int to = 0; to < n; to++) {
                boolean zipperClosedBefore = true;
                for (int tau = 0; tau <= to; tau++) {
                    boolean zipperClosed = bOk[e][to - tau][to];
                    trainingNeeded[e][to - tau][to] =  zipperClosedBefore && zipperClosed  ?  forceScaleOnClosedZipper : (!zipperClosed && zipperClosedBefore ? 1f : 0f);
                    if (alwaysTrainTau1 && tau == 1) trainingNeeded[e][to - tau][to] = zipperClosed  ?  forceScaleOnClosedZipper : 1f;
                    zipperClosedBefore = zipperClosed;
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

    public static int unrollSteps(boolean[][][] b_OK) {
        // sMax is the maximum size minus one of any square b_OK[e] with e = 0, ..., b_OK.length - 1
        int sMax =  IntStream.range(0,b_OK.length).map(e -> b_OK[e].length - 1).max().orElse(0);
        for (int s = 0; s <= sMax; s++) {
            for (int e = 0; e < b_OK.length; e++) {
                for (int t1 = 0; t1 < b_OK[e].length - s   ; t1++) {
                    if (!b_OK[e][t1][t1 + s]) {
                        return s;
                    }
                }
            }
        }
        return sMax + 1;
    }

    public static int maxUnrollSteps(boolean[][][] trainingNeeded) {
        int max = Integer.MIN_VALUE;
        for (int e = 0; e < trainingNeeded.length; e++) {
            for (int t1 = 0; t1 < trainingNeeded[e].length; t1++) {
                for (int t2 = t1; t2 < trainingNeeded[e].length; t2++) {
                    if (trainingNeeded[e][t1][t2]) {
                        max = Math.max(max, t2 - t1);
                    }
                }
            }
        }
        return max;
    }

    public static boolean[][][] b_OK_From_UOk_in_Episodes(List<EpisodeDO> episodeDOList) {
        boolean[][][] b_OK = new boolean[episodeDOList.size() ][][];

        for (int e = 0; e < episodeDOList.size(); e++) {
            EpisodeDO episodeDO = episodeDOList.get(e);
            int tmax = episodeDO.getLastTime() ;
            b_OK[e] = new boolean[tmax + 1][tmax + 1];
            for (int t = 0; t <= tmax; t++) {
                int u = episodeDO.getTimeStep(t).getUOk();
                for (int i = t; i <= t + u; i++) {
                    b_OK[e][t][i] = true;
                }
            }
        }
        return b_OK;
    }

    public static void uOK_in_Episodes_From_b_OK(boolean[][][] bOkBatch, List<EpisodeDO> episodeDOList) {

        for (int e = 0; e < episodeDOList.size(); e++) {
            EpisodeDO episodeDO = episodeDOList.get(e);
            for (int t = 0; t <=  episodeDO.getLastTime(); t++) {
                int u = -1;
                for (int i = t; i <= episodeDO.getLastTime(); i++) {
                    if (bOkBatch[e][t][i]) {
                        u = i - t;
                    } else {
                        break;
                    }
                }
                episodeDO.getTimeStep(t).setUOk(u);
            }
        }
    }




    public static void s_in_Episodes_From_b_OK(boolean[][][] bOkBatch, List<EpisodeDO> episodeDOList) {

        for (int e = 0; e < episodeDOList.size(); e++) {
            EpisodeDO episodeDO = episodeDOList.get(e);
            for (int t = 0; t <=  episodeDO.getLastTimeWithAction()+1; t++) {
                // s
                int s = 0;
                for (int i = 0; i <= t; i++) {
                    if (bOkBatch[e][t - i][t]) {
                        s = i+1;
                    } else {
                        break;
                    }
                }
                TimeStepDO ts = episodeDO.getTimeStep(t);
                ts.setSChanged(ts.getS() != s);
                ts.setS(s);
                ts.setSClosed(s >= t + 1);
                // uOk
                int u = -1;
                for (int i = t; i <= episodeDO.getLastTimeWithAction()+1; i++) {
                    if (bOkBatch[e][t][i]) {
                        u = i - t;
                    } else {
                        break;
                    }
                }
                ts.setUOkChanged(ts.getUOk() != u);
                ts.setUOk(u);
                ts.setUOkClosed(u >= episodeDO.getLastTimeWithAction() - t + 1);

            }
        }
    }


    // stay focused on the timesteps with the given s
    public static List<TimeStepDO> assureThatAMinimumFractionOfTimeStepsAreInBufferForGivenS(List<TimeStepDO> allTimeSteps, double fraction, int u) {
        int n = allTimeSteps.size();
        int maxWithoutS = (int) ((1-fraction) * n);

        List<TimeStepDO> allTimeStepsWithoutS = allTimeSteps.stream().filter(ts -> ts.getS() != u).toList();
        List<TimeStepDO> allTimeStepsWith = allTimeSteps.stream().filter(ts -> ts.getS() == u).toList();

        if (allTimeStepsWithoutS.size() > maxWithoutS) {
            allTimeSteps = new ArrayList<>();
            allTimeSteps.addAll(allTimeStepsWith);
            allTimeSteps.addAll(allTimeStepsWithoutS.subList(0, maxWithoutS));
            Collections.shuffle(allTimeSteps);
        }
        return allTimeSteps;

    }
}
