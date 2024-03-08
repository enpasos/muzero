package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions;

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.training.ParameterStore;
import ai.djl.util.Pair;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.CausalityFreezing;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.Conv3x3;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;

@SuppressWarnings("all")
public class DynamicsStart extends  AbstractBlock implements OnnxIO, CausalityFreezing {

    List<Block> blocks = new ArrayList<>();



 public DynamicsStart(MuZeroConfig config) {
     super(MYVERSION);
     blocks.add(addChildBlock("dynamicsStartRulesInitial", Conv3x3.builder().channels(config.getNumChannelsRulesInitial()).build()));
     blocks.add(addChildBlock("dynamicsStartRulesRecurrent", Conv3x3.builder().channels(config.getNumChannelsRulesRecurrent()).build()));
     blocks.add(addChildBlock("dynamicsStartPolicy", Conv3x3.builder().channels(config.getNumChannelsPolicy()).build()));
     blocks.add(addChildBlock("dynamicsStartValue", Conv3x3.builder().channels(config.getNumChannelsValue()).build()));

    }


    @Override
    public @NotNull String toString() {
        return "RepresentationStart()";
    }

    @Override
    public NDList forward(@NotNull ParameterStore parameterStore, NDList inputs, boolean training) {
        return forward(parameterStore, inputs, training, null);
    }

    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> pairList) {
     NDList result = new NDList();
     IntStream.range(0, blocks.size()).forEach(
             i -> result.add(blocks.get(i).forward(parameterStore, new NDList(inputs.get(i)), training).get(0)));
        result.add(inputs.get(blocks.size()));
        return result;
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        List<Shape> shapes = new ArrayList<>();
        IntStream.range(0, blocks.size()).forEach(i -> shapes.add(blocks.get(i).getOutputShapes(new Shape[] {inputs[i]})[0]));
        shapes.add(inputs[blocks.size()]);
        return shapes.toArray(new Shape[0]);
    }

    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        IntStream.range(0, blocks.size()).forEach(i -> blocks.get(i).initialize(manager, dataType, new Shape[] {inputShapes[i]}));
    }

    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        OnnxBlock onnxBlock = OnnxBlock.builder()
                .input(input)
                .valueInfos(createValueInfoProto(input))
                .build();

        int concatDim = 1;
        List<OnnxTensor> outputsA = new ArrayList<>();
        List<OnnxTensor> outputsB = new ArrayList<>();
        OnnxTensor childOutputA = null;
        OnnxTensor childOutputB = null;
        int c = 0;
        for (Pair<String, Block> p : this.getChildren()) {
            OnnxIO onnxIO = (OnnxIO) p.getValue();

            List<OnnxTensor> myInput = new ArrayList<>();

            OnnxTensor majorInput = input.get(c);
            myInput.add(majorInput);

            OnnxBlock child = onnxIO.getOnnxBlock(counter, myInput);
            onnxBlock.addChild(child);
            childOutputA = child.getOutput().get(0);
            outputsA.add(childOutputA);
            c++;
        }

        // action input
        OnnxTensor extraInput = input.get(blocks.size());
        outputsA.add(extraInput);

        onnxBlock.setOutput(outputsA);

        return onnxBlock;
    }


    @Override
    public void freeze(boolean[] freeze) {
          IntStream.range(0, blocks.size()).forEach(i -> blocks.get(i).freezeParameters(freeze[i]));
    }
}
