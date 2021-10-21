package ai.enpasos.muzero.pegsolitair;

import ai.enpasos.muzero.platform.agent.Inference;
import ai.enpasos.muzero.pegsolitair.config.PegSolitairConfigFactory;
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


//    @Test
//    void aiDecisionSlowForAlreadyFinishedGame() {
//
//        List<Integer> actions = List.of(12, 8, 13, 11, 6, 7, 16, 18, 17, 22, 10, 19, 21, 1, 14, 2, 9, 23, 24, 18, 19, 25, 23, 5, 0, 25, 3, 25, 25);
//
//        int nextMoveInt = Inference.aiDecision(actions, true, "./pretrained", ConfigFactory.getGoInstance(5));
//
//    }
}