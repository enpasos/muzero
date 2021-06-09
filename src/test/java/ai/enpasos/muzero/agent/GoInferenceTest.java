package ai.enpasos.muzero.agent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoInferenceTest {

    @Test
    void aiDecisionFast() {

        boolean withMCTS = false;
        int size = 5;
        List<Integer> actions = new ArrayList<>();

        int nextMoveInt = GoInference.aiDecision(actions, withMCTS, "./memory/go5/networks", size);

    }

    @Test
    void aiDecisionSlow() {

        boolean withMCTS = true;
        int size = 5;
        List<Integer> actions = new ArrayList<>();

        int nextMoveInt = GoInference.aiDecision(actions, withMCTS, "./memory/go5/networks", size);

    }
}
