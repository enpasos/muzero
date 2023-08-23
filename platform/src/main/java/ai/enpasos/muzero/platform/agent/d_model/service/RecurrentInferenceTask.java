package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
public class RecurrentInferenceTask {
    public RecurrentInferenceTask(List<Node> searchPath) {
        epoch = -1;
        this.searchPath = searchPath;
    }
    public RecurrentInferenceTask(List<Node> searchPath, int epoch) {
        this.epoch = epoch;
        this.searchPath = searchPath;
    }

    List<Node> searchPath;
    private NetworkIO networkOutput;
    private boolean done;

    public synchronized boolean isNotDone() {
        return !done;
    }


    private int epoch ;
    public synchronized int getEpoch() {
        return epoch;
    }
    public synchronized NetworkIO getNetworkOutput() {
        return networkOutput;
    }


    public synchronized void setNetworkOutput(NetworkIO networkOutput) {
        this.networkOutput = networkOutput;
    }


    public synchronized void setDone(boolean done) {
        this.done = done;
    }

    public synchronized List<Node> getSearchPath() {
        return searchPath;
    }

}
