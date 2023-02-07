package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.train.MuZero;
import ai.enpasos.muzero.platform.run.train.TrainParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.muzero.platform.common.FileUtils.exists;
import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class InferenceTest {

    @Autowired
    Inference inference;

    @Autowired
    MuZeroConfig config;

    @Autowired
    private MuZero muZero;

    @Test
    void aiDecisionTicTacToeSlow() {
        init();
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, true, null);
        assertTrue(nextMoveInt >= 0);
    }

    private void init() {
        config.setOutputDir("./build/tictactoeTest/");
        rmDir(config.getOutputDir());
        muZero.train(TrainParams.builder()
            .render(true)
            .withoutFill(false)
            .build());
    }


}
