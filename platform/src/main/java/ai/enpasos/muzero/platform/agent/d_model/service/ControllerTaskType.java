package ai.enpasos.muzero.platform.agent.d_model.service;

public enum ControllerTaskType {
    SAVE_LATEST_MODEL, LOAD_LATEST_MODEL, LOAD_LATEST_MODEL_OR_CREATE_IF_NOT_EXISTING, TRAIN_MODEL, TRAIN_MODEL_RULES, GET_EPOCH, START_SCOPE, END_SCOPE
}
