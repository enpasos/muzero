package ai.enpasos.muzero.platform.agent.rational.gumbel;


import ai.enpasos.muzero.platform.common.MuZeroException;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Gumbel {


    public static List<Integer> drawGumbelActionsInitially(double[] policyValues, int n) {
        List<GumbelAction> gumbelActions = getGumbelActions(policyValues);
        int[] actions = gumbelActions.stream().mapToInt(a -> a.getActionIndex()).toArray();
        double[] g = gumbelActions.stream().mapToDouble(a -> a.getGumbelValue()).toArray();
        double[] logits = gumbelActions.stream().mapToDouble(a -> a.getLogit()).toArray();
        return drawActions(actions, add(logits, g), n);
    }

    public static List<GumbelAction> drawGumbelActions(List<GumbelAction> gumbelActions, int n) {
        int[] actions = gumbelActions.stream().mapToInt(a -> a.getActionIndex()).toArray();
        double[] g = gumbelActions.stream().mapToDouble(a -> a.getGumbelValue()).toArray();
        double[] logits = gumbelActions.stream().mapToDouble(a -> a.getLogit()).toArray();
        List<Integer> selectedActions = drawActions(actions, add(logits, g), n);
        return gumbelActions.stream().filter(a -> selectedActions.contains(a.actionIndex)).collect(Collectors.toList());
    }

    public static List<GumbelAction> drawGumbelActionsInitially(List<GumbelAction> gumbelActions, int n) {
        int[] actions = gumbelActions.stream().mapToInt(a -> a.getActionIndex()).toArray();
        double[] g = gumbelActions.stream().mapToDouble(a -> a.getGumbelValue()).toArray();
        double[] logits = gumbelActions.stream().mapToDouble(a -> a.getLogit()).toArray();
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
        // if (n < x.length) throw new MuZeroException("n should not be larger than the number of actions");
        List<Integer> result = new ArrayList<>();

        List<Pair> gPlusLogits = IntStream.range(0, x.length).mapToObj(
            i -> new Pair(i, x[i])
        ).collect(Collectors.toList());

        IntStream.range(0, n).forEach(i -> {
            Pair<Integer, Double> max = gPlusLogits.stream().max((a, b) -> Double.compare((Double) a.getValue(), (Double) b.getValue())).get();
            result.add(actions[max.getKey()]);
            gPlusLogits.remove(max);
        });

        return result;
    }

    public static double[] add(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new MuZeroException("vectors in add operation should have the same length");
        }
        return IntStream.range(0, a.length).mapToDouble(
            i -> a[i] + b[i]
        ).toArray();
    }

    static double[] drawGumble(int k) {
        return IntStream.range(0, k).mapToDouble(i -> drawGumble()).toArray();
    }

    static double drawGumble() {
        double r = 0d;
        while (r == 0d) {
            r = ThreadLocalRandom.current().nextDouble();
        }
        return -Math.log(-Math.log(r));
    }


}
