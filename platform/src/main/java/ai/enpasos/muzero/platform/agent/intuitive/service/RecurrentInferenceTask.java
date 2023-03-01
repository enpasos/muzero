package ai.enpasos.muzero.platform.agent.intuitive.service;

import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.rational.Node;
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
