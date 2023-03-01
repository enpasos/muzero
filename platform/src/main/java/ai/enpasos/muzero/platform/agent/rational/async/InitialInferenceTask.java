package ai.enpasos.muzero.platform.agent.rational.async;

import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
//@Setter
//@Getter
public class InitialInferenceTask {
    public InitialInferenceTask(Game game) {
        this.game = game;
    }

    private Game game;
    private NetworkIO networkOutput;
    private boolean done;


    synchronized
    public Game getGame() {
        return game;
    }

    synchronized
    public void setGame(Game game) {
        this.game = game;
    }

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
