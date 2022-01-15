package ai.enpasos.mnist.inference;

import ai.djl.ndarray.types.Shape;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JvmData {
    String name;
    float[] data;
    Shape shape;
}
