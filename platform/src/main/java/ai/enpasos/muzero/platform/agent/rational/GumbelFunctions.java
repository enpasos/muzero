package ai.enpasos.muzero.platform.agent.rational;

import ai.enpasos.muzero.platform.common.MuZeroException;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.common.Functions.log2;

public class GumbelFunctions {

    private GumbelFunctions() {}


    public static List<GumbelAction> drawGumbelActions(List<GumbelAction> gumbelActions, int n, double temperature) {
        int[] actions = gumbelActions.stream().mapToInt(GumbelAction::getActionIndex).toArray();
        double[] g = gumbelActions.stream().mapToDouble(GumbelAction::getGumbelValue).toArray();
        double[] logits = gumbelActions.stream().mapToDouble(a -> a.getLogit()/temperature).toArray();
        List<Integer> selectedActions = drawActions(actions, add(logits, g), n);
        return gumbelActions.stream().filter(a -> selectedActions.contains(a.actionIndex)).collect(Collectors.toList());
    }

    public static List<GumbelAction> getGumbelActions(double[] policyValues) {
        return IntStream.range(0, policyValues.length).mapToObj(i -> {
            GumbelAction a = GumbelAction.builder()
                .actionIndex(i)
                .policyValue(policyValues[i])
                .build();
            a.initGumbelValue();
            return a;
        }).collect(Collectors.toList());
    }

    public static List<Integer> drawActions(int[] actions, double[] x, int n) {
        List<Integer> result = new ArrayList<>();

        List<Pair<Integer, Double>> gPlusLogits = IntStream.range(0, x.length).mapToObj(
            i -> new Pair<>(i, x[i])
        ).collect(Collectors.toList());

        IntStream.range(0, n).forEach(i -> {
            Pair<Integer, Double> selected = null;
            //if (temperature == 0.0d) {
                selected = gPlusLogits.stream().max(Comparator.comparingDouble(Pair::getValue)).get();
//            } else {
//               double[] vs =  gPlusLogits.stream().mapToDouble(p -> Math.exp(p.getValue()/temperature)).toArray();
//               double sum =  Arrays.stream(vs).sum();
//               double[] ps =  Arrays.stream(vs).map(v -> v/sum).toArray();
//                double rand = ThreadLocalRandom.current().nextDouble();
//                double s = 0d;
//                for (int k = 0; k < ps.length; k++) {
//                    double p = ps[k];
//                    s += p;
//                    if (s > rand) {
//                        selected = gPlusLogits.get(k);
//                        break;
//                    }
//                }
//                if (selected == null)
//                    throw new MuZeroException("problem in drawing from discrete probability distribution");
//            }
            result.add(actions[selected.getKey()]);
            gPlusLogits.remove(selected);
        });

        return result;
    }

    //             this.getChildren().stream().forEach(node -> node.setImprovedPolicyValue3(
    //                Math.exp(node.getImprovedPolicyValue2()/temperature)
    //            ));
    //            double sum = this.getChildren().stream().mapToDouble(Node::getImprovedPolicyValue3).sum();
    //            this.getChildren().stream().forEach(node -> node.setImprovedPolicyValue3(
    //                 node.getImprovedPolicyValue3()/sum
    //            ));
    //            double rand = ThreadLocalRandom.current().nextDouble();
    //            double s = 0d;
    //            for (Node node : this.getChildren()) {
    //                s += node.getImprovedPolicyValue3();
    //                if (s > rand) {
    //                    return node;
    //                }
    //            }
    //            throw new MuZeroException("problem in drawing from discrete probability distribution");

    public static double[] add(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new MuZeroException("vectors in add operation should have the same length");
        }
        return IntStream.range(0, a.length).mapToDouble(
            i -> a[i] + b[i]
        ).toArray();
    }

    static double[] drawGumble(int k ) {
        return IntStream.range(0, k).mapToDouble(i -> drawGumble( )).toArray();
    }

    static double drawGumble( ) {
        double r = 0d;
        while (r == 0d) {
            r = ThreadLocalRandom.current().nextDouble();
        }
        return - Math.log(-Math.log(r));
    }

    public static double[] sigmas(double[] qs, double maxActionVisitCount, int cVisit, double cScale) {
        return Arrays.stream(qs).map(q -> (cVisit + maxActionVisitCount) * cScale * q).toArray();
    }

    public static int[] extraPhaseVisitsToUpdateQPerPhase(int budget, int m) {
        int remainingBudget = budget;
        int phases = (int) log2(m);

        int[] result = new int[phases];
        for (int p = 0; p < phases; p++) {
            int na = (int) ((double) budget / phases /   m);
            if (p < phases - 1) {
                result[p] = na;
                remainingBudget -= na * m;
            } else {
                result[p] = remainingBudget / m;
            }
            m /= 2;
        }
        return result;
    }
}
