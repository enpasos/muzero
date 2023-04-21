package ai.enpasos.muzero.platform.agent.c_model.service;

import ai.enpasos.muzero.platform.agent.c_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.b_planning.Node;
import lombok.ToString;

import java.util.List;

@ToString
public class RecurrentInferenceTask {
    public RecurrentInferenceTask(List<Node> searchPath) {
        this.setSearchPath(searchPath);
    }

    private List<Node> searchPath;
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

    synchronized
    public List<Node> getSearchPath() {
        return searchPath;
    }

    synchronized
    public void setSearchPath(List<Node> searchPath) {
        this.searchPath = searchPath;
    }
}
