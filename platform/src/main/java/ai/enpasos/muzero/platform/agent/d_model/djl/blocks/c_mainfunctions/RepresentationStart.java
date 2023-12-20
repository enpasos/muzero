package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
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

@SuppressWarnings("all")
public class RepresentationStart extends MySequentialBlock {

   private RepresentationStart() {
        super();
    }

    @Builder()
    public static @NotNull RepresentationStart newRepresentationStart(MuZeroConfig config) {

        RepresentationStart block = new RepresentationStart();
        block.add(new ParallelBlockWithCollectChannelJoinExt(
                Arrays.asList(
                        Conv3x3.builder().channels(config.getNumChannelsRules()).build(),
                        Conv3x3.builder().channels(config.getNumChannelsPolicy()).build(),
                        Conv3x3.builder().channels(config.getNumChannelsValue()).build() )
        ));

        return block;

    }



}
