package ai.enpasos.muzero.platform.agent.rational.gumbel;

import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.extraPhaseVisitsToUpdateQPerPhase;
import static org.junit.jupiter.api.Assertions.*;

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

//    @Test
//    void selectActionTest2() {
//        double[] counts = new double[6];
//        for (int i = 0; i < 10000; i++) {
//            double[] logits = {0.0, 0.2, 0.2, 0.1, 0.1, 0.4};
//            double[] ps = softmax(logits);
//            List<GumbelAction> gumbelActions = List.of(
//                GumbelAction.builder()
//                    .actionIndex(0)
//                    .logit(logits[0])
//                    .policyValue(ps[0])
//
//                    .build(),
//                GumbelAction.builder()
//                    .actionIndex(1)
//                    .logit(logits[1])
//                    .policyValue(ps[1])
//                    .build(),
//                GumbelAction.builder()
//                    .actionIndex(2)
//                    .logit(logits[2])
//                    .policyValue(ps[2])
//                    .build(),
//                GumbelAction.builder()
//                    .actionIndex(3)
//                    .logit(logits[3])
//                    .policyValue(ps[3])
//                    .build(),
//                GumbelAction.builder()
//                    .actionIndex(4)
//                    .logit(logits[4])
//                    .policyValue(ps[4])
//                    .build(),
//                GumbelAction.builder()
//                    .actionIndex(5)
//                    .logit(logits[5])
//                    .policyValue(ps[5])
//                    .build()
//            );
//
//            //  List<GumbelAction> gumbelActions = getGumbelActions(new double[]{0.0, 0.2, 0.2, 0.1, 0.1, 0.4});
//            gumbelActions.get(1).setQValue(0.8);
//            gumbelActions.get(2).setQValue(-0.5);
//            GumbelAction action = selectAction(8, 4, gumbelActions, 50, 1.0);
//            counts[action.actionIndex] = counts[action.actionIndex] + 1d;
//        }
//
//        double sum = Arrays.stream(counts).sum();
//        counts = Arrays.stream(counts).map(c -> c / sum).toArray();
//        assertEquals(0d, counts[0]);
//        assertTrue(counts[1] > counts[2]);
//        System.out.println(Arrays.toString(counts));
//    }
}
