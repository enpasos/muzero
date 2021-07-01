package ai.enpasos.muzero.agent;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoInferenceTest {

    @Test
    @Disabled
    void aiDecisionFast() {

        boolean withMCTS = false;
        int size = 5;
        List<Integer> actions = new ArrayList<>();

        int nextMoveInt = GoInference.aiDecision(actions, withMCTS, "./memory/go5/networks", size);

    }

    @Test
    @Disabled
    void aiDecisionSlow() {

        boolean withMCTS = true;
        int size = 5;
        List<Integer> actions = new ArrayList<>();

        int nextMoveInt = GoInference.aiDecision(actions, withMCTS, "./memory/go5/networks", size);

    }


    @Test
    @Disabled
    void aiDecisionSlowLongerGame() {

        boolean withMCTS = true;
        int size = 5;
        List<Integer> actions = List.of(12, 8, 13, 11, 6, 7, 16, 18, 17, 22, 10, 19, 21, 1, 14, 2, 9, 23, 24, 18, 19, 25, 23, 5, 0, 25, 3, 25);

        int nextMoveInt = GoInference.aiDecision(actions, withMCTS, "./memory/go5/networks", size);

    }


    @Test
    @Disabled
    void aiDecisionSlowForAlreadyFinishedGame() {

        boolean withMCTS = true;
        int size = 5;
        List<Integer> actions = List.of(12, 8, 13, 11, 6, 7, 16, 18, 17, 22, 10, 19, 21, 1, 14, 2, 9, 23, 24, 18, 19, 25, 23, 5, 0, 25, 3, 25, 25);

        int nextMoveInt = GoInference.aiDecision(actions, withMCTS, "./memory/go5/networks", size);

    }
}
