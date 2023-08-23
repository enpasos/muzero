package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import lombok.ToString;

@ToString

public class InitialInferenceTask {
    public InitialInferenceTask(Game game) {
        this.game = game;
        epoch = -1;
    }
    public InitialInferenceTask(Game game, int epoch) {
        this.game = game;
        this.epoch = epoch;
    }

    private Game game;
    private NetworkIO networkOutput;
    private boolean done;
    private int epoch ;

    public synchronized int getEpoch() {
        return epoch;
    }

    public synchronized Game getGame() {
        return game;
    }


    public synchronized void setGame(Game game) {
        this.game = game;
    }


    public synchronized NetworkIO getNetworkOutput() {
        return networkOutput;
    }


    public synchronized void setNetworkOutput(NetworkIO networkOutput) {
        this.networkOutput = networkOutput;
    }


    public synchronized boolean isNotDone() {
        return !done;
    }


    public synchronized void setDone(boolean done) {
        this.done = done;
    }
}
