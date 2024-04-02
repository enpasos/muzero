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
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.Conv3x3;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;

@SuppressWarnings("all")
public class DynamicsStart extends  AbstractBlock implements OnnxIO, DCLAware {

    Conv3x3 rulesBlock;
    Conv3x3 policyBlock;
    Conv3x3 valueBlock;


 public DynamicsStart(MuZeroConfig config) {
            super(MYVERSION);

     rulesBlock =  addChildBlock("dynamicsStartRules", Conv3x3.builder().channels(config.getNumChannelsRules()).build());
     policyBlock = addChildBlock("dynamicsStartPolicy", Conv3x3.builder().channels(config.getNumChannelsPolicy()).build());
     valueBlock = addChildBlock("dynamicsStartValue", Conv3x3.builder().channels(config.getNumChannelsValue()).build());

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
        result.add(rulesBlock.forward(parameterStore, new NDList(inputs.get(0)), training).get(0));
        result.add(policyBlock.forward(parameterStore, new NDList(inputs.get(1)), training).get(0));
        result.add(valueBlock.forward(parameterStore, new NDList(inputs.get(2)), training).get(0));
        result.add(inputs.get(3));
        return result;
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        List<Shape> shapes = new ArrayList<>();
        shapes.add(rulesBlock.getOutputShapes(new Shape[] {inputs[0]})[0]);
        shapes.add(policyBlock.getOutputShapes(new Shape[] {inputs[1]})[0]);
        shapes.add(valueBlock.getOutputShapes(new Shape[] {inputs[2]})[0]);
        shapes.add(inputs[3]);
        return shapes.toArray(new Shape[0]);
    }

    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        rulesBlock.initialize(manager, dataType, new Shape[] {inputShapes[0]});
        policyBlock.initialize(manager, dataType, new Shape[] {inputShapes[1]});
        valueBlock.initialize(manager, dataType, new Shape[] {inputShapes[2]});
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
            OnnxTensor extraInput = input.get(3);
        outputsA.add(extraInput);

        onnxBlock.setOutput(outputsA);

        return onnxBlock;
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
