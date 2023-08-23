package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder
@AllArgsConstructor
public class ControllerTask {

    private final ControllerTaskType taskType;
    private NetworkIO networkOutput;
    private volatile boolean done;
    int startEpoch;
    int lastEpoch;


    @Builder.Default
    int epoch = -1;

    public ControllerTask(ControllerTaskType taskType) {
        this.taskType = taskType;
    }


    public synchronized boolean isNotDone() {
        return !done;
    }

    public synchronized void setDone(boolean done) {
        this.done = done;
    }

    public ControllerTaskType getTaskType() {
        return taskType;
    }
}
