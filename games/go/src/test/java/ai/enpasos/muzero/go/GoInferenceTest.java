package ai.enpasos.muzero.go;

import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import org.junit.jupiter.api.Disabled;
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
@Disabled
class GoInferenceTest {

    @Autowired
    Inference inference;

    @Test
    void aiDecisionGoFast() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, false, "./pretrained");
        assertTrue(nextMoveInt >= 0);
    }


    @Test
    void aiDecisionGoSlow() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, true, "./pretrained");
        assertTrue(nextMoveInt >= 0);
    }


    @Test
    void aiDecisionSlowLongerGame() {

        List<Integer> actions = List.of(12, 8, 13, 11, 6, 7, 16, 18, 17, 22, 10, 19, 21, 1, 14, 2, 9, 23, 24, 18, 19, 25, 23, 5, 0, 25, 3, 25);

        int nextMoveInt = inference.aiDecision(actions, true, "./pretrained");

        assertTrue(nextMoveInt >= 0);

    }


}
