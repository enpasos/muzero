package ai.enpasos.muzero.platform.agent.rational.gumbel;


import ai.enpasos.muzero.platform.common.MuZeroException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Gumbel {

    public static Set<Integer> drawGumbleAndActions(double[] pis, int n) {
        double[] g = drawGumble(pis.length);
        double[] logits = getLogits(pis);
        return drawActions(add(logits, g),  n);
    }

    private static double[] getLogits(double[] pis) {
        return Arrays.stream(pis).map(pi -> Math.log(pi / (1 - pi))).toArray();
    }

    public static Set<Integer> drawActions(double[] x, int n) {
        if (n > x.length) throw new MuZeroException("n should not be larger than the number of actions");
        Set<Integer> result = new HashSet<>();

         List<Pair>  gPlusLogits = IntStream.range(0, x.length).mapToObj(
            i -> new Pair(i, x[i])
        ).collect(Collectors.toList());

        IntStream.range(0, n).forEach(i -> {
                Pair<Integer, Double> max = gPlusLogits.stream().max((a,b) -> Double.compare((Double)a.getValue(), (Double)b.getValue())).get();
                result.add(max.getKey());
                gPlusLogits.remove(max);
            });

        return result;
    }

    public static double[] add(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new MuZeroException("vectors in add operation should have the same length");
        }
        return IntStream.range(0, a.length).mapToDouble(
            i ->  a[i] + b[i]
        ).toArray();
    }

    static double[] drawGumble(int k) {
        return IntStream.range(0, k).mapToDouble(i ->  drawGumble()).toArray();
    }
    static double drawGumble() {
        double r = 0d;
        while(r == 0d) {
            r = ThreadLocalRandom.current().nextDouble();
        }
        return -Math.log(-Math.log(r));
    }
}
