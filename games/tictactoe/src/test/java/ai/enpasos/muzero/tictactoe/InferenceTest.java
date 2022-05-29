package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class InferenceTest {

    @Autowired
    Inference inference;

    @Test
    void aiDecisionTicTacToeSlow() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, true, "./pretrained");
        assertTrue(nextMoveInt >= 0);
    }


}
