package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.ParameterStore;
import ai.djl.util.Pair;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.mnist.blocks.ext.ParallelBlockWithCollectChannelJoinExt;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class ParallelBlockWithCollectChannelJoinExtDCLAware extends ParallelBlockWithCollectChannelJoinExt implements DCLAware {

    private  int noOfActiveLayers;

    public int getNoOfActiveLayers() {
        return noOfActiveLayers;
    }

    public void setNoOfActiveLayers(int noOfActiveLayers) {
        this.noOfActiveLayers = noOfActiveLayers;

    }


    @Override
    public void freeze(boolean[] freeze) {
        this.getChildren().forEach(b -> {
            if (b.getValue() instanceof DCLAware) {
                ((DCLAware) b.getValue()).freeze(freeze);
            }
        });
    }

    // workaround as function is private instead of protected in ParallelBlock class
    Function<List<NDList>, NDList> myFunction;


    public ParallelBlockWithCollectChannelJoinExtDCLAware(List<Block> blocks) {
        super(blocks);

         myFunction = list -> {
            List<NDArray> collectedList = list
                    .stream()
                    .map(NDList::head)
                    .collect(Collectors.toList());
            return new NDList(collectedList);
        };
    }


    @Override
    protected NDList forwardInternal(
            ParameterStore parameterStore,
            NDList inputs,
            boolean training,
            PairList<String, Object> params) {
        return myFunction.apply(
                IntStream.range(0, this.noOfActiveLayers).
                        mapToObj(i -> this.children.get(i).getValue().forward(parameterStore, inputs, training, params))
                        .collect(Collectors.toList()));
    }

    @Override
    protected NDList forwardInternal(
            ParameterStore parameterStore,
            NDList data,
            NDList labels,
            PairList<String, Object> params) {
        return myFunction.apply(
                IntStream.range(0, this.noOfActiveLayers).
                        mapToObj(i -> this.children.get(i).getValue().forward(parameterStore, data, labels, params))
                        .collect(Collectors.toList()));
    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        IntStream.range(0, this.noOfActiveLayers).forEach(
                i -> {
                    Block child = this.getChildren().get(i).getValue();
                    child.initialize(manager, dataType, inputShapes);
                }
        );

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
        for (int i = 0; i < this.noOfActiveLayers; i++) {
            Pair<String, Block> p = this.getChildren().get(i);

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
