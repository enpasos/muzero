package ai.enpasos.muzero.platform.agent.rational.gumbel;

import org.apache.commons.math3.util.Pair;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.rational.gumbel.Gumbel.*;

public class SequentialHalving {

    // k: number of actions
    // m<=k: number of actions sampled without replacement
    // n: number of simulations
    // double[] logits: predictor logits from a policy network pi of length k
    public static void run(int k, int m, int n, double[] logits) {
        double[] g = IntStream.range(0, k).mapToDouble(i ->  drawGumble()).toArray();
        Set<Integer> actions = drawActions(add(logits,g),  m );


        int na = (int) ((double)n / log2(m) / (double)m);

      //  double[] qEmpirical

    }

    public static double log2(int n) {
        return  Math.log(n) / Math.log(2);
    }

    public static double sigmoid(double x) {
        return 1d / (1d + Math.exp(-x));
    }
}
