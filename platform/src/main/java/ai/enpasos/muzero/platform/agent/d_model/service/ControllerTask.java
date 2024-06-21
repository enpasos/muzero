package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.config.TrainingDatasetType;
import lombok.ToString;

import java.util.List;

@ToString

public class ControllerTask {
    private boolean background;
    private final ControllerTaskType taskType;
    private NetworkIO networkOutput;
    private volatile boolean done;

    boolean[] freeze = new boolean[3];

    private boolean[] exportFilter = {true, true, true};
    TrainingDatasetType trainingDatasetType;

    int epoch = -1;
    private List<Integer> uOkList;

public ControllerTask(ControllerTaskType taskType) {
    this.taskType = taskType;
}



    public synchronized boolean isDone() {
        return !done;
    }

    public synchronized void setDone(boolean done) {
        this.done = done;
    }



    public ControllerTaskType getTaskType() {
        return taskType;
    }

    public void setFreeze(boolean[] freeze) {
        this.freeze = freeze;
    }

    public TrainingDatasetType getTrainingDatasetType() {
    return trainingDatasetType;
    }

    public void setTrainingDatasetType(TrainingDatasetType trainingDatasetType) {
    this.trainingDatasetType = trainingDatasetType;
    }

    public boolean isBackground() {
        return background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public void setExportFilter(boolean[] exportFilter) {
        this.exportFilter = exportFilter;
    }

    public boolean[] getExportFilter() {
        return exportFilter;
    }


    public List<Integer> getuOkList() {
        return uOkList;
    }

    public void setuOkList(List<Integer> uOkList) {
        this.uOkList = uOkList;
    }
}
