package ai.enpasos.muzero.platform.agent.rational.gumbel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GumbelInfoTest {


    @Test
    void initGumbelInfo() {

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
        GumbelInfo gumbelInfo =  GumbelInfo.initGumbelInfo(n, m, k);
        int sum = 0;
        for (int i = 0; i < gumbelInfo.getExtraVisitsPerPhase().length; i++) {
            int v = gumbelInfo.getExtraVisitsPerPhase()[i];
            sum += m *v;
            m /= 2;
        }
        return sum;
    }
}
