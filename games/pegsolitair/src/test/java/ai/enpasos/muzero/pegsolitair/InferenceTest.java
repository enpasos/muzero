package ai.enpasos.muzero.pegsolitair;

import ai.enpasos.muzero.pegsolitair.config.PegSolitairConfigFactory;
import ai.enpasos.muzero.platform.agent.Inference;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class InferenceTest {

    @Test
    @Ignore
    public void aiDecisionTicTacToeSlow() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = Inference.aiDecision(actions, true, "./pretrained", PegSolitairConfigFactory.getSolitairInstance());
    }


}
