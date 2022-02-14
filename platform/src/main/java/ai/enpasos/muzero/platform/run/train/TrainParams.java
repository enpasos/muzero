package ai.enpasos.muzero.platform.run.train;

import ai.djl.Model;
import lombok.Builder;
import lombok.Data;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Data
@Builder
public class TrainParams {

    @Builder.Default
    BiConsumer<Integer, Model> hookIn = (epoch, model) -> {};

    @Builder.Default
    boolean freshBuffer = false;

    @Builder.Default
    int numberOfEpochs = 1;

    @Builder.Default
    boolean render = false;

    @Builder.Default
    boolean randomFill = true;








}
