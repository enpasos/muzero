package ai.enpasos.muzero.platform.agent.memorize;

import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest
class GameTest {

    @Autowired
    MuZeroConfig config;


    @Test
    void getTdStepsTest() {
        Game game = config.newGame();
        game.setPRatioMax(1);
        // action space: 0, 1, 2
        int T = 3;
        game.apply(0);
        game.getGameDTO().getPlayoutPolicy().add(new float[] {0.1f, 0.2f, 0.7f});
        game.getGameDTO().getPolicyTargets().add(new float[] {0.1f, 0.2f, 0.7f});

        game.apply(1);
        game.getGameDTO().getPlayoutPolicy().add(new float[] {0.1f, 0.2f, 0.7f});
        game.getGameDTO().getPolicyTargets().add(new float[] {0.1f, 0.2f, 0.7f});

        game.apply(2);
        game.getGameDTO().getPlayoutPolicy().add(new float[] {0f, 0f, 1f});
        game.getGameDTO().getPolicyTargets().add(new float[] {0f, 0f, 1f});

        assertEquals(2, game.getTdSteps(0.9f, 0, T));
        assertEquals(0, game.getTdSteps(0.9f, T, T));

    }


    @Test
    void getTdStepsTest2() {
        Game game = config.newGame();
        game.setPRatioMax(1.0);
        // action space: 0, 1, 2
        int T = 3;
        game.apply(0);
        game.getGameDTO().getPlayoutPolicy().add(new float[] {0.1f, 0.2f, 0.7f});
        game.getGameDTO().getPolicyTargets().add(new float[] {0.9f*0.1f, 0.9f*0.2f, 0.9f*0.7f});

        game.apply(1);
        game.getGameDTO().getPlayoutPolicy().add(new float[] {0.1f, 0.2f, 0.7f});
        game.getGameDTO().getPolicyTargets().add(new float[] {0.9f*0.1f, 0.9f*0.2f, 0.9f*0.7f});

        game.apply(2);
        game.getGameDTO().getPlayoutPolicy().add(new float[] {0f, 0f, 1f});
        game.getGameDTO().getPolicyTargets().add(new float[] {0.9f*0f, 0.9f*0f, 0.9f*1f});

//        pRatio:
//        0.8099999061226886  = 0.9 * 0.9 = 0.81
//        0.8999999478459366  = 0.9
//        1.0

        assertEquals(2, game.getTdSteps(0.7f, 0, T));
        assertEquals(1, game.getTdSteps(0.82f, 0, T));
        assertEquals(0, game.getTdSteps(0.91f, 0, T));

        assertEquals(0, game.getTdSteps(0.9f, T, T));


        game.setPRatioMax(2.0);
        assertThrows(MuZeroNoSampleMatch.class, () -> game.getTdSteps(0.91f, 0, T));

    }

}
