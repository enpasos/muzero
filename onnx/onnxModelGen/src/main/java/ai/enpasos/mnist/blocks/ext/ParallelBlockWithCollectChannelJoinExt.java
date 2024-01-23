package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.nn.Block;
import ai.djl.nn.ParallelBlock;
import ai.djl.util.Pair;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

@SuppressWarnings("all")
public class ParallelBlockWithCollectChannelJoinExt extends ParallelBlock implements OnnxIO {


    public ParallelBlockWithCollectChannelJoinExt(List<Block> blocks) {
        super(

            list -> {
                List<NDArray> collectedList = list
                    .stream()
                    .map(NDList::head)
                    .collect(Collectors.toList());

                return new NDList(collectedList);
            }


            , blocks);
    }


    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .valueInfos(createValueInfoProto(input))
            .build();

        int concatDim = 1;
        List<OnnxTensor> output = new ArrayList<>();
        OnnxTensor childOutput = null;
        for (Pair<String, Block> p : this.getChildren()) {
            OnnxIO onnxIO = (OnnxIO) p.getValue();
            OnnxBlock child = onnxIO.getOnnxBlock(counter, input);
            onnxBlock.addChild(child);
            if (child.getOutput().size() > 1)
                throw new RuntimeException("each output is assumed to be a single tensor here");
            childOutput = child.getOutput().get(0);
            output.add(childOutput);
        }


        onnxBlock.setOutput(output);

        return onnxBlock;
    }


}
