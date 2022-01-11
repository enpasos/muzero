package ai.enpasos.mnist.blocks;

import ai.djl.ndarray.types.Shape;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OnnxTensor {
    private String name;
    private Shape shape;

}
