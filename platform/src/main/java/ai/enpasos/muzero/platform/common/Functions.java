package ai.enpasos.muzero.platform.common;

import ai.enpasos.muzero.platform.agent.rational.Action;
import org.apache.commons.math3.util.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class Functions {

    private Functions() {}

    public static double[] softmax(double[] raw) {
        double max = Arrays.stream(raw).max().getAsDouble();
        raw = Arrays.stream(raw).map(x -> x - max).toArray();
        double[] vs = Arrays.stream(raw).map(Math::exp).toArray();
        double sum = Arrays.stream(vs).sum();
        return Arrays.stream(vs).map(v -> v / sum).toArray();
    }

    public static double[] toDouble(float[] ps) {
        return IntStream.range(0, ps.length).mapToDouble(i -> ps[i]).toArray();
    }
    public static double entropy(double[] ps) {
        return Arrays.stream(ps).reduce(0d, (e, p) -> e - p * Math.log(p));
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

}
