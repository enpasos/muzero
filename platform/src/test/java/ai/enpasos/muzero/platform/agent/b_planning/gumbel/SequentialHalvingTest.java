package ai.enpasos.muzero.platform.agent.b_planning.gumbel;

import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.platform.agent.b_planning.GumbelFunctions.extraPhaseVisitsToUpdateQPerPhase;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class SequentialHalvingTest {


    @Test
    void extraPhaseVisitsToUpdateQTest() {
        int[] extraPhaseVisitsToUpdateQ = extraPhaseVisitsToUpdateQPerPhase(200, 16);
        int[] expected = {3, 6, 12, 28};
        assertArrayEquals(expected, extraPhaseVisitsToUpdateQ);
    }

    @Test
    void extraPhaseVisitsToUpdateQTest2() {
        int[] extraPhaseVisitsToUpdateQ = extraPhaseVisitsToUpdateQPerPhase(50, 8);
        int[] expected = {2, 4, 9};
        assertArrayEquals(expected, extraPhaseVisitsToUpdateQ);
    }

}
