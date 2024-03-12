package ai.enpasos.muzero.platform.agent.d_model.djl.blocks;

public interface DCLAware {  // TODO: refactor to a configuration object that is passed to the blocks
    void freeze(boolean[] freeze);

     int getNoOfActiveLayers() ;

     void setNoOfActiveLayers(int noOfActiveLayers);
}
