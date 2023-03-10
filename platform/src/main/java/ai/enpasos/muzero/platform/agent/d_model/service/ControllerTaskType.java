package ai.enpasos.muzero.platform.agent.d_model.service;

public enum ControllerTaskType {
    loadLatestModel, loadLatestModelOrCreateIfNotExisting, trainModel, getEpoch, startScope, endScope
}
