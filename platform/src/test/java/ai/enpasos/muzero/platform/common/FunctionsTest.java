package ai.enpasos.muzero.platform.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FunctionsTest {

    private static double[] sm(double[] raw, double temperature) {
        double[] softmax1 = Functions.softmax(raw, temperature);
        assertEquals(1d, Arrays.stream(softmax1).sum(), 0.000001d);
        return softmax1;
    }

    @Test
    void numpyRandomDirichlet() {
        double alpha = 3d;
        double[] pOld = {1d / 3, 1d / 3, 1d / 3};
        for (int i = 0; i < 10; i++) {
            double[] p = Functions.numpyRandomDirichlet(alpha, 3);
            assertEquals(1d, Arrays.stream(p).sum(), 0.000001d);
            assertFalse(Arrays.equals(p, pOld));
            System.out.println(Arrays.toString(p));
            pOld = p;
        }
    }

    @Test
    void softmax() {
        double[] raw = {1d, 2d, 3d, 4d};
        System.out.println(Arrays.toString(raw));
        System.out.println(Arrays.toString(sm(raw, 0.01)));
        System.out.println(Arrays.toString(sm(raw, 0.1)));
        System.out.println(Arrays.toString(sm(raw, 1)));
        System.out.println(Arrays.toString(sm(raw, 2)));
        System.out.println(Arrays.toString(sm(raw, 5)));
    }
}
