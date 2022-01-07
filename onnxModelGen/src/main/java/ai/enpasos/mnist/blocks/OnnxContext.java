package ai.enpasos.mnist.blocks;

import ai.djl.ndarray.types.Shape;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OnnxContext {
    int counter;
    List<String> inputNames;
    List<Shape> inputShapes;

    public int counter() {
        return counter++;
    }
}
