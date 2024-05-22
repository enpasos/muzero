package ai.enpasos.muzero.platform.agent.d_model.djl;

import lombok.Data;

@Data
public class Statistics {
    private int count;
    private double sumLossLegalActions;
    private double sumLossReward;
}
