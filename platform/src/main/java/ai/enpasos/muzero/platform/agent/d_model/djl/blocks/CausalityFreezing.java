package ai.enpasos.muzero.platform.agent.d_model.djl.blocks;

public interface CausalityFreezing {
    void freeze(boolean[] freeze);
}
