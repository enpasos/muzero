package ai.enpasos.mnist.blocks.ext;

import ai.djl.nn.Block;
import ai.djl.nn.SequentialBlock;
import ai.djl.util.Pair;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;

import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class SequentialBlockExt extends SequentialBlock implements OnnxIO {
    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {

        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .valueInfos(createValueInfoProto(input))
            .build();

        List<OnnxTensor> currentInput = input;
        for (Pair<String, Block> p : this.getChildren()) {
            OnnxIO onnxIO = (OnnxIO) p.getValue();
            OnnxBlock child = onnxIO.getOnnxBlock(counter, currentInput);
            onnxBlock.addChild(child);

            currentInput = child.getOutput();
        }

        onnxBlock.setOutput(currentInput);

        return onnxBlock;
    }
}
