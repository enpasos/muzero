package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.Inference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest()
public class InferenceTest {

    @Autowired
    Inference inference;

//    @Test
//    @Ignore
//    public void aiDecisionTicTacToeSlow() {
//        List<Integer> actions = new ArrayList<>();
//        int nextMoveInt = inference.aiDecision(actions, true, "./pretrained");
//    }


}
