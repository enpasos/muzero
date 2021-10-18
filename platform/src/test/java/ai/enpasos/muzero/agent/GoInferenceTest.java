package ai.enpasos.muzero.agent;

import ai.enpasos.muzero.MuZeroConfig;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

class GoInferenceTest {

    @Test
    void aiDecisionGoFast() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = Inference.aiDecision(actions, false, "./pretrained", MuZeroConfig.getGoInstance(5));
    }

    @Test
    void aiDecisionTicTacToeFast() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = Inference.aiDecision(actions, false, "./pretrained", MuZeroConfig.getTicTacToeInstance());
    }
    @Test
    void aiDecisionTicTacToeSlow() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = Inference.aiDecision(actions, true, "./pretrained", MuZeroConfig.getTicTacToeInstance());
    }

    @Test
    void aiDecisionGoSlow() {
        List<Integer> actions = new ArrayList<>();
        int nextMoveInt = Inference.aiDecision(actions, true, "./pretrained", MuZeroConfig.getGoInstance(5));
    }


    @Test
    void aiDecisionSlowLongerGame() {

        List<Integer> actions = List.of(12, 8, 13, 11, 6, 7, 16, 18, 17, 22, 10, 19, 21, 1, 14, 2, 9, 23, 24, 18, 19, 25, 23, 5, 0, 25, 3, 25);

        int nextMoveInt = Inference.aiDecision(actions, true, "./pretrained", MuZeroConfig.getGoInstance(5));

    }


//    @Test
//    void aiDecisionSlowForAlreadyFinishedGame() {
//
//        List<Integer> actions = List.of(12, 8, 13, 11, 6, 7, 16, 18, 17, 22, 10, 19, 21, 1, 14, 2, 9, 23, 24, 18, 19, 25, 23, 5, 0, 25, 3, 25, 25);
//
//        int nextMoveInt = Inference.aiDecision(actions, true, "./pretrained", MuZeroConfig.getGoInstance(5));
//
//    }
}
