package ai.enpasos.muzero.platform.config;

import ai.enpasos.muzero.platform.common.MuZeroException;
import org.apache.commons.math3.util.Pair;

import java.util.Comparator;
import java.util.stream.IntStream;

public class ValueConverter {

    private ValueConverter() {}
    public static int valueToClassIndex(MuZeroConfig config, double value) {
        return valueToClassIndex(config.getValues(), value);
    }

    public static int valueToClassIndex(double[] confValues, double value) {
        return (Integer) IntStream.range(0, confValues.length)
            .mapToObj(i -> new Pair(i, confValues[i]))
            .min(Comparator.comparing(p -> Math.abs((Double) p.getValue() - value)))
            .orElseThrow(() -> new MuZeroException("No value present"))
            .getFirst();
    }

    public static double expectedValue(MuZeroConfig config, float[] ps) {
        return expectedValue(config.getValues(), ps);
    }

    public static double expectedValue(double[] confValues, float[] ps) {
        if (confValues.length != ps.length) throw new MuZeroException("arrays need to have same size");
        return IntStream.range(0, confValues.length).mapToDouble(i -> confValues[i] * ps[i]).sum();
    }


}
