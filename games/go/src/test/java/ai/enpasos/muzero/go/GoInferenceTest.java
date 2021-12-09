package ai.enpasos.muzero.go;

import ai.enpasos.muzero.platform.agent.Inference;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest()
public class GoInferenceTest {

    @Autowired
    Inference inference;

    @Test
    @Ignore
    void aiDecisionGoFast() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, false, "./pretrained");
    }


    @Test
    @Ignore
    void aiDecisionGoSlow() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, true, "./pretrained");
    }


    @Test
    @Ignore
    void aiDecisionSlowLongerGame() {

        List<Integer> actions = List.of(12, 8, 13, 11, 6, 7, 16, 18, 17, 22, 10, 19, 21, 1, 14, 2, 9, 23, 24, 18, 19, 25, 23, 5, 0, 25, 3, 25);

        int nextMoveInt = inference.aiDecision(actions, true, "./pretrained");

    }


}
