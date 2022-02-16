package ai.enpasos.muzero.platform.run.train;

import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import lombok.Builder;
import lombok.Data;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Data
@Builder
public class TrainParams {

    @Builder.Default
    BiConsumer<Integer, Model> afterTrainingHookIn = (epoch, model) -> {};


    @Builder.Default
    Consumer<Network> afterSelfPlayHookIn = (network) -> {};

    @Builder.Default
    boolean freshBuffer = false;

    @Builder.Default
    int numberOfEpochs = 1;

    @Builder.Default
    boolean render = false;

    @Builder.Default
    boolean randomFill = true;








}
