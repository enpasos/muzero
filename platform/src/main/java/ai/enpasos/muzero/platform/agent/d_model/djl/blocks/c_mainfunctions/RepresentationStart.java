package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions;

import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.mnist.blocks.ext.ParallelBlockWithCollectChannelJoinExt;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.Conv3x3;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;

@SuppressWarnings("all")
public class RepresentationStart extends  AbstractBlock implements OnnxIO, DCLAware {



    ParallelBlockWithCollectChannelJoinExt block;
    Conv3x3 rulesBlock;
    Conv3x3 policyBlock;
    Conv3x3 valueBlock;


 public RepresentationStart(MuZeroConfig config) {
            super(MYVERSION);

     rulesBlock = Conv3x3.builder().channels(config.getNumChannelsRules()).build();
     policyBlock = Conv3x3.builder().channels(config.getNumChannelsPolicy()).build();
     valueBlock = Conv3x3.builder().channels(config.getNumChannelsValue()).build();


        block = addChildBlock("rS", new ParallelBlockWithCollectChannelJoinExt(
                Arrays.asList(
                        rulesBlock ,
                        policyBlock,
                        valueBlock )
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


    @Override
    public void freezeParameters(boolean[] freeze) {
        rulesBlock.freezeParameters(freeze[0]);
        policyBlock.freezeParameters(freeze[1]);
        valueBlock.freezeParameters(freeze[2]);
    }


    @Override
    public void setExportFilter(boolean[] exportFilter) {
        rulesBlock.setStoring(exportFilter[0]);
        policyBlock.setStoring(exportFilter[1]);
        valueBlock.setStoring(exportFilter[2]);
    }
}
