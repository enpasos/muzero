package ai.enpasos.muzero.platform.agent.e_experience.memory;

import lombok.Data;

@Data
public class StateNodeAction {

        StateNode stateNode;
        int action;
}
