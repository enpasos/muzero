package ai.enpasos.muzero.platform.common;

import ai.enpasos.muzero.platform.agent.rational.Action;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import umontreal.ssj.randvarmulti.DirichletGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class Functions {

    private static final RandomGenerator rng = new Well19937c();
    private static final RandomStream randomStreamBase = new MRG32k3a("rnd");

    private Functions() {
    }

    public static double @NotNull [] numpyRandomDirichlet(double alpha, int dims) {

        double[] alphas = new double[dims];
        Arrays.fill(alphas, alpha);
        DirichletGen dg = new DirichletGen(randomStreamBase, alphas);
        double[] p = new double[dims];

        dg.nextPoint(p);
        return p;
    }

    public static double[] softmax(double[] raw) {
        return softmax(raw, 1.0);
    }

    public static double[] softmax(double[] raw, double temperature) {
        if (temperature == 0) {
            return softmax0(raw);
        }
        double max = Arrays.stream(raw).max().getAsDouble();
        raw = Arrays.stream(raw).map(x -> (x - max) / temperature).toArray();
        double[] vs = Arrays.stream(raw).map(Math::exp).toArray();
        double sum = Arrays.stream(vs).sum();
        return Arrays.stream(vs).map(v -> v / sum).toArray();
    }

    private static double[] softmax0(double[] raw) {
        double[] result = new double[raw.length];
        int maxi = 0;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] > max) {
                maxi = i;
                max = raw[i];
            }
        }
        result[maxi] = 1.0;
        return result;
    }

    public static double[] toDouble(float[] ps) {
        return IntStream.range(0, ps.length).mapToDouble(i -> ps[i]).toArray();
    }

    public static double entropy(double[] ps) {
        return Arrays.stream(ps).reduce(0d, (e, p) -> e + singleEntropySummand(p));
    }

    public static double entropy(List<Float> ps) {
        return ps.stream().reduce(0f, (e, p) -> e + (float) singleEntropySummand(p));
    }

    public static double singleEntropySummand(double p) {
        if (p == 0 || p == 1) {
            return 0;
        }
        return -p * Math.log(p);
    }

    public static Action selectActionByMaxFromDistribution(List<Pair<Action, Double>> distributionInput) {
        Collections.shuffle(distributionInput);
        return distributionInput.stream()
            .max(Comparator.comparing(Pair::getSecond))
            .orElseThrow(MuZeroException::new).getKey();
    }

    public static double log2(int n) {
        return Math.log(n) / Math.log(2);
    }

    public static Action selectActionByDrawingFromDistribution(List<Pair<Action, Double>> distributionInput) {
        EnumeratedDistribution<Action> distribution;
        try {
            distribution = new EnumeratedDistribution<>(rng, distributionInput);
        } catch (Exception e) {
            throw new MuZeroException(e);
        }
        return distribution.sample();
    }

    public static int draw(double[] ps) {
        double rand = ThreadLocalRandom.current().nextDouble();
        double s = 0d;
        for (int i = 0; i < ps.length; i++) {
            s += ps[i];
            if (s >= rand) {
                return i;
            }
        }
        throw new MuZeroException("problem in drawing from discrete probability distribution");
    }

    public static boolean draw(double p) {
        double rand = ThreadLocalRandom.current().nextDouble();
        return rand < p;
    }


}
