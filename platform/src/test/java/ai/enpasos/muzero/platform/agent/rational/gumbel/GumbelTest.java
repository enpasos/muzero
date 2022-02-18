package ai.enpasos.muzero.platform.agent.rational.gumbel;

import ai.enpasos.muzero.platform.common.MuZeroException;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.platform.agent.rational.gumbel.Gumbel.add;
import static org.junit.jupiter.api.Assertions.*;

class GumbelTest {


    @Test
    void addTest() {
        double[] a = {0.2, 0.7, 0.1};
        double[] b = {0.1, 0.2, 0.3};
        double[] c = {0.3, 0.9, 0.4};
        assertArrayEquals(c, add(a, b), 1e-5);
    }



    @Test
    void drawActions() {
        double[] pis = {0.2, 0.7, 0.1};
        assertEquals( 3, Gumbel.drawGumbleAndActions(pis, 3).size());
        assertEquals(2, Gumbel.drawGumbleAndActions(pis, 2).size());
        assertEquals(1, Gumbel.drawGumbleAndActions(pis, 1).size());
        assertEquals(0, Gumbel.drawGumbleAndActions(pis, 0).size());
        assertThrows(MuZeroException.class, () -> {
            Gumbel.drawGumbleAndActions(pis, 4);
        });
        System.out.println(Gumbel.drawGumbleAndActions(pis, 2).toString());
        System.out.println(Gumbel.drawGumbleAndActions(pis, 2).toString());
        System.out.println(Gumbel.drawGumbleAndActions(pis, 2).toString());
        System.out.println(Gumbel.drawGumbleAndActions(pis, 2).toString());
        System.out.println(Gumbel.drawGumbleAndActions(pis, 2).toString());
        System.out.println(Gumbel.drawGumbleAndActions(pis, 2).toString());
        System.out.println(Gumbel.drawGumbleAndActions(pis, 2).toString());
    }
}
