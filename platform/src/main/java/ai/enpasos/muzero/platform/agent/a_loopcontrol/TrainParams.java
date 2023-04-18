package ai.enpasos.muzero.platform.agent.a_loopcontrol;

import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.d_model.Network;
import lombok.Builder;
import lombok.Data;

import java.util.function.BiConsumer;

@Data
@Builder
public class TrainParams {
    @Builder.Default
    public boolean withoutFill = false;

    @Builder.Default
    BiConsumer<Integer, Model> afterTrainingHookIn = (epoch, model) -> {
    };


    @Builder.Default
    BiConsumer<Integer, Network> afterSelfPlayHookIn = (epoch, network) -> {
    };

    @Builder.Default
    boolean doNotLoadLatestState = false;


    @Builder.Default
    boolean render = false;

    @Builder.Default
    boolean randomFill = true;


}
