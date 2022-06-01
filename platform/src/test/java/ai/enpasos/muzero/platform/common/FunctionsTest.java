package ai.enpasos.muzero.platform.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FunctionsTest {

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
}
