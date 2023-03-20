package ai.enpasos.muzero.go;

import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.DeviceType;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class GoInferenceTest {

    @Autowired
    Inference inference;

    @Autowired
    MuZeroConfig config;

    @Autowired
    private MuZeroLoop muZero;

    @Autowired
    ModelService modelService;

    @Test
    void aiDecisionGoFast() {
        init();
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, false, null,  DeviceType.GPU);
        assertTrue(nextMoveInt >= 0);

    }

    private void init() {
        config.setOutputDir("./build/goTest/");
        rmDir(config.getOutputDir());
        try {
            modelService.loadLatestModelOrCreateIfNotExisting().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new MuZeroException(e);
        }
    }


    @Test
    void aiDecisionGoSlow() {
        init();
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, true,  null,  DeviceType.GPU);
        assertTrue(nextMoveInt >= 0);

    }


    @Test
    void aiDecisionSlowLongerGame() {
        init();
        List<Integer> actions = List.of(12, 8, 13, 11, 6, 7, 16, 18, 17, 22, 10, 19, 21, 1, 14, 2, 9, 23, 24, 18, 19, 25, 23, 5, 0, 25, 3, 25);
        int nextMoveInt = inference.aiDecision(actions, true,  null,  DeviceType.GPU);
        assertTrue(nextMoveInt >= 0);

    }

    @Test
    void aiDecisionFastLongerGame2() {
        init();
        List<Integer> actions = List.of(12, 16);
        int nextMoveInt = inference.aiDecision(actions, false, null,  DeviceType.GPU);
        assertTrue(nextMoveInt >= 0);

    }


}
