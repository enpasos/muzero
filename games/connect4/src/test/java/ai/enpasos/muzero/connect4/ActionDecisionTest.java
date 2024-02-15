package ai.enpasos.muzero.connect4;

import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
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
@Slf4j
@Disabled
class ActionDecisionTest {

    @Autowired
    Inference inference;

    @Autowired
    MuZeroConfig config;

    @Autowired
    private MuZeroLoop muZero;

    @Autowired
    ModelService modelService;

    @Test
    void aiDecisionConnect4Slow() throws ExecutionException, InterruptedException {

        init();
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, true, null);
        assertTrue(nextMoveInt >= 0);

    }

    @Test
    void aiDecisionConnect4Fast() throws ExecutionException, InterruptedException {
        init();
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, false, null);
        assertTrue(nextMoveInt >= 0);

    }


    private void init() throws ExecutionException, InterruptedException {
        config.setOutputDir("./build/connect4Test/");
        rmDir(config.getOutputDir());
        modelService.loadLatestModelOrCreateIfNotExisting().get();
    }


}
