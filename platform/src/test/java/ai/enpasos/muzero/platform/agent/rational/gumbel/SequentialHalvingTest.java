package ai.enpasos.muzero.platform.agent.rational.gumbel;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static ai.enpasos.muzero.platform.agent.rational.gumbel.Gumbel.getGumbelActions;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.SequentialHalving.extraPhaseVisitsToUpdateQPerPhase;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.SequentialHalving.selectAction;
import static org.junit.jupiter.api.Assertions.*;

class SequentialHalvingTest {


    @Test
    void extraPhaseVisitsToUpdateQTest() {
        int[] extraPhaseVisitsToUpdateQ = extraPhaseVisitsToUpdateQPerPhase(200, 16);
        int[] expected = {3, 6, 12, 28};
        assertArrayEquals(expected, extraPhaseVisitsToUpdateQ);
    }




    @Test
    void selectActionTest() {
        double[] counts = new double[6];
        for (int i = 0; i < 10000; i++) {
            GumbelAction action = selectAction(6, 4, new double[]{0.0, 0.2, 0.2, 0.1, 0.1, 0.4});
            counts[action.actionIndex] = counts[action.actionIndex] + 1d;
        }

        double sum = Arrays.stream(counts).sum();
        counts = Arrays.stream(counts).map(c -> c/sum).toArray();
        assertEquals(0d, counts[0]);
        assertTrue(counts[5] >= 4*counts[4]);
        assertTrue(counts[1] >= 2*counts[3]);
        System.out.println(Arrays.toString(counts));
    }

    @Test
    void selectActionTest2() {
        double[] counts = new double[6];
        for (int i = 0; i < 10000; i++) {
            List<GumbelAction> gumbelActions = getGumbelActions(new double[]{0.0, 0.2, 0.2, 0.1, 0.1, 0.4});
            gumbelActions.get(1).setQ(0.8);
            gumbelActions.get(2).setQ(-0.5);
            GumbelAction action = selectAction(8, 4, gumbelActions);
            counts[action.actionIndex] = counts[action.actionIndex] + 1d;
        }

        double sum = Arrays.stream(counts).sum();
        counts = Arrays.stream(counts).map(c -> c/sum).toArray();
        assertEquals(0d, counts[0]);
        assertTrue(counts[1] > counts[2]);
        System.out.println(Arrays.toString(counts));
    }
}
