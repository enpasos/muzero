package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.ParallelBlock;
import ai.djl.util.Pair;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;
import static ai.enpasos.mnist.blocks.OnnxBlock.getNames;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class ParallelBlockWithAddJoinExt extends ParallelBlock implements OnnxIO {


    public ParallelBlockWithAddJoinExt(List<Block> blocks) {
        super(
            list -> {
            NDList unit = list.get(0);
            NDList parallel = list.get(1);
            return new NDList(
                unit.singletonOrThrow()
                    .add(parallel.singletonOrThrow())
            );
        },  blocks);
    }



    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .valueInfos(createValueInfoProto(input))
            .build();

        int concatDim = 1;
        List<OnnxTensor> outputsToBeCombined = new ArrayList<>();
     //   long size = 0; // along the concatenation dim
        OnnxTensor childOutput = null;
        for (Pair<String, Block> p : this.getChildren()) {
            OnnxIO onnxIO = (OnnxIO)p.getValue();
            OnnxBlock child = onnxIO.getOnnxBlock(counter, input);
            onnxBlock.addChild(child);
            if (child.getOutput().size() > 1) throw new RuntimeException("each output is assumed to be a single tensor here");
            childOutput = child.getOutput().get(0);
            outputsToBeCombined.add(childOutput);
          //  size += childOutput.getShape().get(concatDim);
        }

       Shape inputShapeExample = childOutput.getShape();
        List<OnnxTensor> output = combine(List.of("T" + counter.count()), List.of(
            inputShapeExample
        ));


        OnnxBlock concatBlock = OnnxBlock.builder()
            .input(outputsToBeCombined)
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("Add")
                    .addAllInput(getNames(outputsToBeCombined))
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .valueInfos(createValueInfoProto(output))
            .build();

        onnxBlock.addChild(concatBlock);
        onnxBlock.setOutput(concatBlock.getOutput());

        return onnxBlock;
    }
}
