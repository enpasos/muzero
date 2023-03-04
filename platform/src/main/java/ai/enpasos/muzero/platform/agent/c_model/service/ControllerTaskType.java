package ai.enpasos.muzero.platform.agent.c_model.service;

public enum ControllerTaskType {
    loadLatestModel, loadLatestModelOrCreateIfNotExisting, trainModel, getEpoch, startScope, endScope, shutdown
}
