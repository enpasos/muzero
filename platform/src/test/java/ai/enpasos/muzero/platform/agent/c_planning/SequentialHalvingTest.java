package ai.enpasos.muzero.platform.agent.c_planning;

import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.platform.agent.c_planning.SequentialHalving.extraPhaseVisitsToUpdateQPerPhase;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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


    @Test
    void extraPhaseVisitsToUpdateQTest3() {
        int[] extraPhaseVisitsToUpdateQ = extraPhaseVisitsToUpdateQPerPhase(20, 9);
        int[] expected = {1, 1, 2, 3};
        assertArrayEquals(expected, extraPhaseVisitsToUpdateQ);
    }



    @Test
    void nextTest() {
        int n= 20;
        SequentialHalving sequentialHalfing = SequentialHalving.initGumbelInfo(n, 9, 9);
        int c = 0;
        while(!sequentialHalfing.finished) {
            sequentialHalfing.next();
            c++;
        }
        assertEquals(20, c);
     }




    @Test
    void initSequentialHalving() {


        simNumTest(20, 4, 9);

        simNumTest(30, 16, 26);

        // from paper for go
        simNumTest(200, 16, 26);


        // tictactoe example
        simNumTest(20, 4, 9);

        // go example
        simNumTest(50, 16, 26);

        simNumTest(50, 16, 16);
    }

    private void simNumTest(int n, int m, int k) {
        int s;
        s = usedSimulationsNum(n, m, k);
        assertEquals(n, s);
    }

    private int usedSimulationsNum(int n, int m, int k) {
        SequentialHalving sequentialHalfingInfo = SequentialHalving.initGumbelInfo(n, m, k);
        int sum = 0;
        for (int i = 0; i < sequentialHalfingInfo.getExtraVisitsPerPhase().length; i++) {
            int v = sequentialHalfingInfo.getExtraVisitsPerPhase()[i];
            sum += m * v;
            m /= 2;
        }
        return sum;
    }
}
