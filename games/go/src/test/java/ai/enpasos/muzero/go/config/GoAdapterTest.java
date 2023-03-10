package ai.enpasos.muzero.go.config;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.go.config.environment.basics.move.Pass;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.play.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class GoAdapterTest {


    @Autowired
    MuZeroConfig config;

    @Test
    void translatePass() {
        Action action = GoAdapter.translate(config, new Pass());
        NDArray ndArray = action.encode(NDManager.newBaseManager());
        assertNotNull(ndArray);
    }


    @Test
    void someTest() {
        Action action = new GoAction(config, 3);
        NDArray ndArray = action.encode(NDManager.newBaseManager());
        System.out.println(Arrays.toString(ndArray.toFloatArray()));

        List<Float> actionList = new ArrayList<>();
        int s = config.getActionSpaceSize();
        for (int i = 0; i < s - 1; i++) {
            if (i == action.getIndex()) {
                actionList.add(1f);
            } else {
                actionList.add(0f);
            }
        }
        double[] a2 = actionList.stream().mapToDouble(f -> f).toArray();
        System.out.println(Arrays.toString(a2));

        assertEquals(Arrays.toString(ndArray.toFloatArray()), Arrays.toString(a2));
    }

    @Test
    void translatePassAction() {
        Action action = GoAdapter.translate(config, new Pass());
        NDArray ndArray = action.encode(NDManager.newBaseManager());
        assertNotNull(ndArray);
        assertArrayEquals(new float[config.getBoardWidth() * config.getBoardHeight()], ndArray.toFloatArray());
    }
}
