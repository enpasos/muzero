package ai.enpasos.muzero.platform.agent.c_model.service;

import ai.enpasos.muzero.platform.agent.c_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.b_planning.Node;
import ai.enpasos.muzero.platform.agent.d_experience.Game;
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


    synchronized
    public NetworkIO getNetworkOutput() {
        return networkOutput;
    }

    synchronized
    public void setNetworkOutput(NetworkIO networkOutput) {
        this.networkOutput = networkOutput;
    }

    synchronized
    public boolean isDone() {
        return done;
    }

    synchronized
    public void setDone(boolean done) {
        this.done = done;
    }


}
