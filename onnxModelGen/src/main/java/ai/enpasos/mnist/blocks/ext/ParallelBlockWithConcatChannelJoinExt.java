package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.nn.Block;
import ai.djl.util.Pair;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.djl.nn.ParallelBlock;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ai.enpasos.mnist.blocks.OnnxBlock.*;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class ParallelBlockWithConcatChannelJoinExt extends ParallelBlock implements OnnxIO {


    public ParallelBlockWithConcatChannelJoinExt(List<Block> blocks) {
        super(list -> {
            List<NDArray> concatenatedList =
                list.stream().map(NDList::head).collect(Collectors.toList());
            return new NDList(NDArrays.concat(new NDList(concatenatedList), 1));
        }, blocks);
    }


    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .valueInfos(createValueInfoProto(input))
            .build();

        List<OnnxTensor> outputsToBeConcatenated = new ArrayList<>();
        for (Pair<String, Block> p : this.getChildren()) {
            OnnxIO onnxIO = (OnnxIO)p.getValue();
            OnnxBlock child = onnxIO.getOnnxBlock(counter, input);
            onnxBlock.addChild(child);
            if (child.getOutput().size() > 1) throw new RuntimeException("each output is assumed to be a single tensor here");
            outputsToBeConcatenated.add(child.getOutput().get(0));
        }


        List<OnnxTensor> output = createOutput(List.of("T" + counter.count()), input, this::getOutputShapes);

        OnnxBlock concatBlock = OnnxBlock.builder()
            .input(outputsToBeConcatenated)
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("Concat")
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INT)
                        .setName("axis")
                        .setI(1)
                        .build())
                    .addAllInput(getNames(outputsToBeConcatenated))
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
