package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.Inference;
import ai.enpasos.muzero.tictactoe.config.TicTacToeConfigFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class InferenceTest {

    @Test
    public void aiDecisionTicTacToeSlow() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = Inference.aiDecision(actions, true, "./pretrained", TicTacToeConfigFactory.getTicTacToeInstance());
    }


}
