package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static ai.enpasos.muzero.platform.common.Functions.ln;
import static ai.enpasos.muzero.platform.common.Functions.softmax;
import static ai.enpasos.muzero.platform.common.Functions.toDouble;
import static ai.enpasos.muzero.platform.common.Functions.toFloat;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest
class GameTest {

    @Autowired
    MuZeroConfig config;


    @Test
    void getTdStepsTest() {
        Game game = config.newGame(true, true);
        game.setPRatioMax(1);
        // action space: 0, 1, 2
        int T = 3;
        TimeStepDO  timeStepDO = game.getEpisodeDO().getLastTimeStep();
        timeStepDO.setPlayoutPolicy(new float[] {0.7f, 0.1f, 0.1f});
        timeStepDO.setPolicyTarget(new float[] {0.7f, 0.1f, 0.1f});
        game.apply(0);

        timeStepDO = game.getEpisodeDO().getLastTimeStep();
        timeStepDO.setPlayoutPolicy(new float[] {0.0f, 0.8f, 0.2f});
        timeStepDO.setPolicyTarget(new float[] {0.0f, 0.8f, 0.2f});
        game.apply(1);

        timeStepDO = game.getEpisodeDO().getLastTimeStep();
        timeStepDO.setPlayoutPolicy(new float[] {0f, 0f, 1f});
        timeStepDO.setPolicyTarget(new float[] {0f, 0f, 1f});
        game.apply(2);

        assertEquals(3, game.getTdSteps(0.9f, 0, T));
        assertEquals(0, game.getTdSteps(0.9f, T, T));

    }

    @Test
    void softmaxLnTest() {
        double[] ps = {0.1, 0.1, 0.8};
        double[] result = softmax(ln(ps));
        // assertArrayEquals but allow for small differences
        for (int i = 0; i < result.length; i++) {
            assertEquals(ps[i], result[i], 0.0000001);
        }
    }


    @Test
    void getTdStepsTest2() {
        Game game = config.newGame(true, true);
        game.setPRatioMax(1.5);
        // action space: 0, 1, 2
        int T = 3;
        double temperature = 2;
        float[] ps = {0.7f, 0.1f, 0.1f};
        float[] ps2 = toFloat(softmax(ln(toDouble(ps)), temperature));
        TimeStepDO  timeStepDO = game.getEpisodeDO().getLastTimeStep();
        timeStepDO.setPlayoutPolicy(ps2);
        timeStepDO.setPolicyTarget(ps);
        game.apply(0);

        ps = new float[] {0.0f, 0.8f, 0.2f};
        ps2 = toFloat(softmax(ln(toDouble(ps)), temperature));
        timeStepDO = game.getEpisodeDO().getLastTimeStep();
        timeStepDO.setPlayoutPolicy(ps2);
        timeStepDO.setPolicyTarget(ps);
        game.apply(1);

        ps = new float[] {0f, 0f, 1f};
        ps2 = toFloat(softmax(ln(toDouble(ps)), temperature));
        timeStepDO = game.getEpisodeDO().getLastTimeStep();
        timeStepDO.setPlayoutPolicy(ps2);
        timeStepDO.setPolicyTarget(ps);
        game.apply(2);



        assertEquals(3, game.getTdSteps(0.7f, 0, T));
        assertEquals(3, game.getTdSteps(0.82f, 0, T));
        assertEquals(3, game.getTdSteps(0.91f, 0, T));

        assertEquals(0, game.getTdSteps(0.9f, T, T));


        game.setPRatioMax(2.0);
        assertThrows(MuZeroNoSampleMatch.class, () -> game.getTdSteps(0.91f, 0, T));

    }

}
