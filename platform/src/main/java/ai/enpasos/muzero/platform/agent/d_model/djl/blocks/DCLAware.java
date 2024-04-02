package ai.enpasos.muzero.platform.agent.d_model.djl.blocks;

public interface DCLAware {
    void freezeParameters(boolean[] freeze);

    void setExportFilter(boolean[] exportFilter);
}
