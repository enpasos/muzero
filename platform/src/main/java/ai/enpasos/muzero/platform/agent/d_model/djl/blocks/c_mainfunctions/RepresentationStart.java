package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions;

import ai.djl.ndarray.NDArray;
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
import ai.enpasos.mnist.blocks.ext.ParallelBlockWithAddJoinExt;
import ai.enpasos.mnist.blocks.ext.ParallelBlockWithCollectChannelJoinExt;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.Conv3x3;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;
import static ai.enpasos.mnist.blocks.OnnxBlock.getNames;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;

@SuppressWarnings("all")
public class RepresentationStart extends  AbstractBlock implements OnnxIO {



    ParallelBlockWithCollectChannelJoinExt block;


 public RepresentationStart(MuZeroConfig config) {
            super(MYVERSION);
        block = addChildBlock("representationStart", new ParallelBlockWithCollectChannelJoinExt(
                Arrays.asList(
                        Conv3x3.builder().channels(config.getNumChannelsRules()).build(),
                        Conv3x3.builder().channels(config.getNumChannelsPolicy()).build(),
                        Conv3x3.builder().channels(config.getNumChannelsValue()).build() )
        ));
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
        return block.forward(parameterStore, inputs, training);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        List<Shape> shapes = new ArrayList<>();
        for (Block myblock : block.getChildren().values()) {
            shapes.add(myblock.getOutputShapes(inputs)[0]);
        }
        return shapes.toArray(new Shape[0]);
    }

    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        block.initialize(manager, dataType, inputShapes);
    }

    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        return block.getOnnxBlock(counter, input);
    }

}
