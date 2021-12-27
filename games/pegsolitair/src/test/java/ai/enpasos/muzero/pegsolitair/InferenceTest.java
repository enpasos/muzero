package ai.enpasos.muzero.pegsolitair;

import ai.enpasos.muzero.platform.agent.Inference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import java.util.ArrayList;
import java.util.List;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Disabled
public class InferenceTest {

    @Autowired
    Inference inference;

    @Test
    public void aiDecisionTicTacToeSlow() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = inference.aiDecision(actions, true, "./pretrained");
    }


}
