package ai.enpasos.mnist.blocks;

import java.util.List;

public interface OnnxIO {
    OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input);
}
