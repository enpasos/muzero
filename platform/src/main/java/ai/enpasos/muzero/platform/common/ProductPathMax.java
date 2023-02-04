package ai.enpasos.muzero.platform.common;

public class ProductPathMax {



   // TODO: start with naive implementation then performance improve!!!!!
    public static double getProductPathMax(double[] values) {
        int n = values.length;
        double max = 0.0;

        for (int start = 0; start < n; start++) {
            for (int end = 0; end < n; end++) {
                double p = 1.0;
                for (int i = start; i <= end; i++) {
                    p *= values[i];
                }
                max = Math.max(max, p);
            }
        }
        return max;
    }
}
