/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package ai.enpasos.muzero.platform;


import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class EnumeratedIntegerDistributionTest {

    /**
     * The distribution object used for testing.
     */
    private final @NotNull EnumeratedIntegerDistribution testDistribution;

    /**
     * Creates the default distribution object used for testing.
     */
    public EnumeratedIntegerDistributionTest() throws Exception {
        // Non-sorted singleton array with duplicates should be allowed.
        // Values with zero-probability do not extend the support.
        testDistribution = new EnumeratedIntegerDistribution(
            new int[]{3, -1, 3, 7, -2, 8},
            new double[]{0.2, 0.2, 0.3, 0.3, 0.0, 0.0});
    }

    /**
     * Tests if the distribution returns proper probability values.
     */
    @Test
    void testProbability() {
        int[] points = new int[]{-2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8};
        double[] results = new double[]{0, 0.2, 0, 0, 0, 0.5, 0, 0, 0, 0.3, 0};
        for (int p = 0; p < points.length; p++) {
            double probability = testDistribution.probability(points[p]);
            assertEquals(probability, results[p], 0.0);
        }
    }

    /**
     * Tests if the distribution returns proper cumulative probability values.
     */
    @Test
    void testCumulativeProbability() {
        int[] points = new int[]{-2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8};
        double[] results = new double[]{0, 0.2, 0.2, 0.2, 0.2, 0.7, 0.7, 0.7, 0.7, 1.0, 1.0};
        for (int p = 0; p < points.length; p++) {
            double probability = testDistribution.cumulativeProbability(points[p]);
            assertEquals(probability, results[p], 1e-10);
        }
    }

    /**
     * Tests if the distribution returns proper mean value.
     */
    @Test
    void testGetNumericalMean() {
        assertEquals(3.4, testDistribution.getNumericalMean(), 1e-10);
    }

    /**
     * Tests if the distribution returns proper variance.
     */
    @Test
    void testGetNumericalVariance() {
        assertEquals(7.84, testDistribution.getNumericalVariance(), 1e-10);
    }

    /**
     * Tests if the distribution returns proper lower bound.
     */
    @Test
    void testGetSupportLowerBound() {
        assertEquals(-1, testDistribution.getSupportLowerBound());
    }

    /**
     * Tests if the distribution returns proper upper bound.
     */
    @Test
    void testGetSupportUpperBound() {
        assertEquals(7, testDistribution.getSupportUpperBound());
    }

    /**
     * Tests if the distribution returns properly that the support is connected.
     */
    @Test
    void testIsSupportConnected() {
        assertTrue(testDistribution.isSupportConnected());
    }

    /**
     * Tests sampling.
     */
    @Test
    void testSample() {
        final int n = 1000000;
        testDistribution.reseedRandomGenerator(-334759360); // fixed seed
        final int[] samples = testDistribution.sample(n);
        assertEquals(samples.length, n);
        double sum = 0;
        double sumOfSquares = 0;
        for (int sample : samples) {
            sum += sample;
            sumOfSquares += sample * sample;
        }
        assertEquals(1e-2, sum / n, testDistribution.getNumericalMean()
        );
    }

}
