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
import ai.enpasos.mnist.blocks.ext.ParallelBlockWithCollectChannelJoinExt;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.Conv3x3;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.ParallelBlockWithCollectChannelJoinExtDCLAware;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;

@SuppressWarnings("all")
public class RepresentationStart extends  AbstractBlock implements OnnxIO, DCLAware {

    private  int noOfActiveLayers;

    public int getNoOfActiveLayers() {
        return noOfActiveLayers;
    }

    public void setNoOfActiveLayers(int noOfActiveLayers) {
        this.noOfActiveLayers = noOfActiveLayers;
        for (Pair<String, Block> child : this.children) {
            if(child.getValue() instanceof DCLAware dclAware) {
                dclAware.setNoOfActiveLayers(noOfActiveLayers);
            }
        }
    }

    Block parentBlock;

    List<Block> childBlocks = new ArrayList<>();

    public RepresentationStart() {
        super(MYVERSION);
    }

    Block rulesInitialBlock;


    public RepresentationStart(MuZeroConfig config) {
        this();

        this.rulesInitialBlock = Conv3x3.builder().channels(config.getNumChannelsRulesInitial()).build();

        childBlocks.add(this.rulesInitialBlock);
        childBlocks.add(Conv3x3.builder().channels(config.getNumChannelsRulesRecurrent()).build());
        childBlocks.add(Conv3x3.builder().channels(config.getNumChannelsPolicy()).build());
        childBlocks.add(Conv3x3.builder().channels(config.getNumChannelsValue()).build());

        parentBlock = addChildBlock("representationStart", new ParallelBlockWithCollectChannelJoinExtDCLAware(
                childBlocks
        ));
        setNoOfActiveLayers(4); // default
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
        return parentBlock.forward(parameterStore, inputs, training);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        List<Shape> shapes = new ArrayList<>();
        IntStream.range(0, this.noOfActiveLayers).forEach(
                i -> {Block myBlock = parentBlock.getChildren().values().get(i);
                    shapes.add(myBlock.getOutputShapes(inputs)[0]);
                }
        );
        return shapes.toArray(new Shape[0]);
    }

    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        parentBlock.initialize(manager, dataType, inputShapes);
    }

    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        return ((OnnxIO)parentBlock).getOnnxBlock(counter, input);
    }


    @Override
    public void freeze(boolean[] freeze) {
        IntStream.range(0, childBlocks.size()).forEach(i -> {
            if (childBlocks.get(i) instanceof DCLAware) {
                ((DCLAware) childBlocks.get(i)).freeze(freeze);
            }
        });
    }

//    public RepresentationStart getBlockForInitialRulesOnly() {
//        RepresentationStart block2 = new RepresentationStart();
//
//        block2.childBlocks.add(rulesInitialBlock);
//        block2.parentBlock = block2.addChildBlockPublic("representationStartForInitialRulesOnly", rulesInitialBlock);
//        return block2;
//    }

    private Block addChildBlockPublic(String name, Block block) {
        return this.addChildBlock(name, block);
    }
}
