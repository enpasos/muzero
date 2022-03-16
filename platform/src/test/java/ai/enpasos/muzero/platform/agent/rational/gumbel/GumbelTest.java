package ai.enpasos.muzero.platform.agent.rational.gumbel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.enpasos.muzero.platform.agent.rational.EpisodeManager.softmax;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.Gumbel.add;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GumbelTest {


    @Test
    void addTest() {
        double[] a = {0.2, 0.7, 0.1};
        double[] b = {0.1, 0.2, 0.3};
        double[] c = {0.3, 0.9, 0.4};
        assertArrayEquals(c, add(a, b), 1e-5);
    }


    @Test
    void drawActionsTest() {
        double[] logits = {0.2, 0.7, 0.1};
        double[] ps = softmax(logits);
        List<GumbelAction> actions = List.of(
            GumbelAction.builder()
                .actionIndex(0)
                .logit(logits[0])
                .policyValue(ps[0])
                .qValue(0.1)
                .build(),
            GumbelAction.builder()
                .actionIndex(1)
                .logit(logits[1])
                .policyValue(ps[1])
                .qValue(0.2)
                .build(),
            GumbelAction.builder()
                .actionIndex(2)
                .logit(logits[2])
                .policyValue(ps[2])
                .qValue(0.3)
                .build()
        );


        assertEquals(2, Gumbel.drawGumbelActions(actions, 2).size());
        assertEquals(3, Gumbel.drawGumbelActions(actions, 3).size());
        assertEquals(1, Gumbel.drawGumbelActions(actions, 1).size());
        assertEquals(0, Gumbel.drawGumbelActions(actions, 0).size());
//        assertThrows(MuZeroException.class, () -> {
//            Gumbel.drawGumbelActionsInitially(actions, 4);
//        });
        System.out.println(Gumbel.drawGumbelActions(actions, 2).toString());
        System.out.println(Gumbel.drawGumbelActions(actions, 2).toString());
        System.out.println(Gumbel.drawGumbelActions(actions, 2).toString());
        System.out.println(Gumbel.drawGumbelActions(actions, 2).toString());
        System.out.println(Gumbel.drawGumbelActions(actions, 2).toString());
        System.out.println(Gumbel.drawGumbelActions(actions, 2).toString());
    }


}
