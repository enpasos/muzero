package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
@Setter
@Getter
public class RecurrentInferenceTask {
    public RecurrentInferenceTask(List<Node> searchPath) {
        this.searchPath = searchPath;
    }

    List<Node> searchPath;
    private NetworkIO networkOutput;
    private boolean done;




}
