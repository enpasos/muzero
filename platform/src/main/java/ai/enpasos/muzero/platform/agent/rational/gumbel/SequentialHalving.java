package ai.enpasos.muzero.platform.agent.rational.gumbel;

import ai.enpasos.muzero.platform.common.MuZeroException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.agent.rational.gumbel.Gumbel.*;

public class SequentialHalving {

    public static int getM(int n) {
        return Math.min(n, 16);
    }

    public static GumbelAction selectAction(int n, int m,  double[] policyValues) {
        if(m > policyValues.length) throw new MuZeroException("m must not be larger than number of actions");
        List<GumbelAction> gumbelActions = getGumbelActions(policyValues);
        return selectAction(n, m, gumbelActions);
    }

    public static GumbelAction selectAction(int n, int m,  List<GumbelAction> gumbelActions) {
         gumbelActions = drawGumbelActionsInitially(gumbelActions, m);
        int[] extraPhaseVisitsToUpdateQPerPhase = extraPhaseVisitsToUpdateQPerPhase(n, m);
        for (int p = 0; p < extraPhaseVisitsToUpdateQPerPhase.length; p++) {
            int extraPhaseVisitsToUpdateQ = extraPhaseVisitsToUpdateQPerPhase[p];
            updateQs(extraPhaseVisitsToUpdateQ);
            m /= 2;
            gumbelActions = drawGumbelActions( gumbelActions,  m);
        }
        return gumbelActions.get(0);
    }

    public static void updateQs(int extraPhaseVisitsToUpdateQ) {
        // here just a placeholder
    }

    public static List<GumbelAction> drawGumbelActions(List<GumbelAction> gumbelActions, int m) {
        int[] actions = gumbelActions.stream().mapToInt(a -> a.getActionIndex()).toArray();
        double[] g = gumbelActions.stream().mapToDouble(a -> a.getGumbelValue()).toArray();
        double[] logits = gumbelActions.stream().mapToDouble(a -> a.getLogit()).toArray();
        double[] qs = gumbelActions.stream().mapToDouble(a -> a.getQ()).toArray();
        int maxActionVisitCount = gumbelActions.stream().mapToInt(a -> a.getVisitCount()).max().getAsInt();
        double[] sigmas = sigmas(qs, maxActionVisitCount);
        Set<Integer> selectedActions = drawActions(actions, add(add(logits, g),sigmas),  m);
        return gumbelActions.stream().filter(a -> selectedActions.contains(a.actionIndex)).collect(Collectors.toList());
    }



    public static double log2(int n) {
        return  Math.log(n) / Math.log(2);
    }

    public static double[] sigmas(double[] qs, double maxActionVisitCount) {
        double cVisit = 50;
        double cScale = 10;
        return Arrays.stream(qs).map(q-> (cVisit + maxActionVisitCount) * cScale * q).toArray();
    }

    public static boolean isPowerOfTwo(int x) {
        return (x & (x - 1)) == 0;
    }

    static int[] extraPhaseVisitsToUpdateQPerPhase(int budget, int m) {
       // if (! isPowerOfTwo(m)) throw new MuZeroException("m must be a power of 2");
        int remainingBudget = budget;
        int phases = (int)log2(m);

        int[] result = new int[phases];
        for(int p = 0; p < phases; p++) {
            int na = (int) ((double) budget / phases / (double) m);
            if (p < phases - 1) {
                result[p] = na;
                remainingBudget -= na * m;
            } else {
                result[p] = remainingBudget/m;
            }
            m /= 2;
        }
        return result;
    }
}
