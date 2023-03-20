package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import lombok.ToString;

@ToString

public class ControllerTask {

    private final ControllerTaskType taskType;
    private NetworkIO networkOutput;
    private volatile boolean done;



    int epoch = -1;

public ControllerTask(ControllerTaskType taskType) {
    this.taskType = taskType;
}


    synchronized
    public boolean isDone() {
        return !done;
    }

    synchronized
    public void setDone(boolean done) {
        this.done = done;
    }

    public ControllerTaskType getTaskType() {
        return taskType;
    }
}
