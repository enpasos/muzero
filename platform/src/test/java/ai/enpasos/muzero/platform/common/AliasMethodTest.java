package ai.enpasos.muzero.platform.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AliasMethodTest {

    @Test
    void sample() {
        double[] weights = {0.1, 0.2, 0.3, 0.4};
        AliasMethod aliasMethod = new AliasMethod(weights);
        int[] counts = new int[weights.length];
        int n = 100000;
        for (int i = 0; i < n; i++) {
            counts[aliasMethod.sample()]++;
        }
        for (int i = 0; i < weights.length; i++) {
            double expected = weights[i] * n;
            double actual = counts[i];
            double error = Math.abs(expected - actual);
            assertTrue(error < 0.01 * n, "Error too large, expected: " + expected + ", actual: " + actual);
        }
    }

    @Test
    void sampleWithoutReplacement() {
        double[] weights = {0.1, 0.8, 1.6, 3.2};
        AliasMethod aliasMethod = new AliasMethod(weights);
 ;
        List<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        list.add("D");

        int[] samples = aliasMethod.sampleWithoutReplacement(4);
         Set<Integer> set = new HashSet<>();
        for (int i = 0; i < samples.length; i++) {
            set.add(samples[i]);
        }
        assertEquals(4, set.size());


    }
}
