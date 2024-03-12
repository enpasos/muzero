package ai.enpasos.muzero.platform.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static ai.enpasos.muzero.platform.common.Functions.dotProduct;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void softmax2() {
        double[] raw = {1d,  7d};
        System.out.println(Arrays.toString(raw));
        System.out.println(Arrays.toString(sm(raw, 1)));
    }

    @Test
    void dotProductTest() {
        double[] a = {1d, 2d, 3d};
        double[] b = {1d, 0.5d, 1d/3d};
    assertEquals(3d, dotProduct(a,b), 0.000001d);
    }

    @Test
    void rescaleLogitsIfOutsideInterval() {
        double[] a = {0.9, 0.5, 0.5};
        double[] b = Functions.rescaleLogitsIfOutsideInterval(a, 5d);
        System.out.println(Arrays.toString(b));
        assertArrayEquals(a, b, 0.000001d);

        a = new double[] {Math.log(0.999), Math.log(0.005), Math.log(0.005)};
        System.out.println(Arrays.toString(a));
        b = Functions.rescaleLogitsIfOutsideInterval(a, 5d);
        System.out.println(Arrays.toString(b));
        //assertArrayEquals(a, b, 0.000001d);
    }

//    @Test
//    void rescaleLogitsIfOutsideInterval2() {
//        double maxScaleInterval = 5d;
//        double[] a = {Double.NEGATIVE_INFINITY, 1.0};
//        double[] b = Functions.rescaleLogitsIfOutsideInterval(a, maxScaleInterval);
//
//        double[] c = {0.0, maxScaleInterval};
//        System.out.println(Arrays.toString(b));
//        assertArrayEquals(a, b, 0.000001d);
//
//    }


}
