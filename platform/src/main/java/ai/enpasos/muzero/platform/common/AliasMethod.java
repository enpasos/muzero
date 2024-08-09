package ai.enpasos.muzero.platform.common;

import java.util.*;

public class AliasMethod {
    private final int[] alias;
    private final double[] prob;
    private final int n;
    private final Random random;

    public AliasMethod(double[] weights) {
        n = weights.length;
        alias = new int[n];
        prob = new double[n];
        random = new Random();

        // Normalize weights to sum to n
        double sum = 0;
        for (double weight : weights) {
            sum += weight;
        }
        double[] normalizedWeights = new double[n];
        for (int i = 0; i < n; i++) {
            normalizedWeights[i] = weights[i] * n / sum;
        }

        // Small and large stacks
        int[] small = new int[n];
        int[] large = new int[n];
        int smallIndex = 0, largeIndex = 0;

        for (int i = 0; i < n; i++) {
            if (normalizedWeights[i] < 1.0) {
                small[smallIndex++] = i;
            } else {
                large[largeIndex++] = i;
            }
        }

        // Distribute probabilities
        while (smallIndex > 0 && largeIndex > 0) {
            int smallIdx = small[--smallIndex];
            int largeIdx = large[--largeIndex];

            prob[smallIdx] = normalizedWeights[smallIdx];
            alias[smallIdx] = largeIdx;

            normalizedWeights[largeIdx] = normalizedWeights[largeIdx] + normalizedWeights[smallIdx] - 1.0;

            if (normalizedWeights[largeIdx] < 1.0) {
                small[smallIndex++] = largeIdx;
            } else {
                large[largeIndex++] = largeIdx;
            }
        }

        while (largeIndex > 0) {
            prob[large[--largeIndex]] = 1.0;
        }

        while (smallIndex > 0) {
            prob[small[--smallIndex]] = 1.0;
        }
    }

    public int sample() {
        int column = random.nextInt(n);
        boolean coinToss = random.nextDouble() < prob[column];
        return coinToss ? column : alias[column];
    }

    public static void main(String[] args) {
        double[] weights = {0.1, 0.9}; // Example weights
        AliasMethod aliasMethod = new AliasMethod(weights);
        int draws = 10;
        for (int i = 0; i < draws; i++) {
            System.out.println(aliasMethod.sample());
        }
    }

    public int[] sampleWithoutReplacement(int n) {
        int[] result = new int[n];
        TreeSet<Integer> set = new TreeSet<>();
        while(set.size() < n) {
            set.add(sample());
        }
        return set.stream().mapToInt(i -> i).toArray();
    }
}
