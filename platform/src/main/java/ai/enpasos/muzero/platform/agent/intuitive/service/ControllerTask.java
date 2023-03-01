package ai.enpasos.muzero.platform.agent.intuitive.service;

import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import lombok.ToString;

@ToString

public class ControllerTask {

    private ControllerTaskType taskType;
    private NetworkIO networkOutput;
    private boolean done;

public ControllerTask(ControllerTaskType taskType) {
    this.taskType = taskType;
}


    synchronized
    public boolean isDone() {
        return done;
    }

    synchronized
    public void setDone(boolean done) {
        this.done = done;
    }

    public ControllerTaskType getTaskType() {
        return taskType;
    }
}
