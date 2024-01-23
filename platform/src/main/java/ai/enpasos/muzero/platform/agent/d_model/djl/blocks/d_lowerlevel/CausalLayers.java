package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel;

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
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.CausalityFreezing;
import ai.enpasos.muzero.platform.common.MuZeroException;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

@SuppressWarnings("all")
public class CausalLayers extends AbstractBlock implements OnnxIO, CausalityFreezing {

    private final List<Block> layers;


    @Override
    public void freeze(boolean[] freeze) {
        for(int i = 0; i < freeze.length; i++) {
            layers.get(i).freezeParameters(freeze[i]);
        }
    }


    // Layers are ordered bottom up in the list
    // Each layer may depend on the output of the previous layers (with s)
    // There is no backpropagation across layer boundaries
    public CausalLayers(List<Block> layers ) {
        this.layers = layers;
        for(Block layer: layers) {
            this.addChildBlock("CausalLayer", layer);
        }

    }


    // The inputs are the major inputs to the layers
    // They are ordered the same way as the layers (bottom up)
    // The outputs are the outputs of the layers again ordered bottom up
    // The inputs to the layers are ordered again bottom up (the major one is the last one)
    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {
        NDList combinedResult = new NDList();
        for(int k = 0; k < layers.size(); k++) {
            Block layer = layers.get(k);
            NDList layerInput = new NDList();
            for(int j = 0; j < k; j++) {
                // There is no backpropagation across layer boundaries
                NDArray minorInput = inputs.get(j).stopGradient();
                layerInput.add(minorInput);
            }
            NDArray majorInput = inputs.get(k);
            layerInput.add(majorInput);

            // The last input is an extra input (action) that is only used by the first layer (rules layer)
            if ( layers.size() + 1 == inputs.size()) {
                NDArray extraInput = inputs.get(inputs.size() - 1);
                layerInput.add(0, extraInput);
            }

            NDArray resultArray = layer.forward(parameterStore, layerInput, training, params).get(0);
            combinedResult.add(resultArray);
        }
        return combinedResult;
    }



    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] outputShapes = new Shape[layers.size()];
        for (int i = 0; i < layers.size(); i++) {
            outputShapes[i] = inputShapes[i];
        }
        return outputShapes;
    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        if (layers.size() > inputShapes.length) {
            throw new MuZeroException("number of layers must be less or equal to number of inputs");
        }

        for (int k = 0; k < layers.size(); k++) {
            List<Shape> layerInputShapes = new ArrayList<>();

            for(int j = 0; j < k; j++) {
                Shape inputShape = inputShapes[j];
                layerInputShapes.add(inputShape);
            }
            Shape majorInputShape = inputShapes[k];
            layerInputShapes.add(majorInputShape);

            // The first input is an extra input (action) - the identity function is later on the last input ... the "majorInput"
            if ( layers.size() + 1 == inputShapes.length) {
                Shape extraInputShape = inputShapes[inputShapes.length - 1];
                layerInputShapes.add(0, extraInputShape);
            }
            layers.get(k).initialize(manager, dataType,  layerInputShapes.toArray(new Shape[0]));
        }
    }




    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .valueInfos(createValueInfoProto(input))
            .build();

        int concatDim = 1;
        List<OnnxTensor> outputsToBeConcatenated = new ArrayList<>();
        long size = 0; // along the concatenation dim
        OnnxTensor childOutput = null;
        int c = 0;
        for (Pair<String, Block> p : this.getChildren()) {
            OnnxIO onnxIO = (OnnxIO) p.getValue();



            List<OnnxTensor> myInput = new ArrayList<>();
            for(int j = 0; j < c; j++) {
                OnnxTensor minorInput = input.get(j);
                myInput.add(minorInput);
            }
            OnnxTensor majorInput = input.get(c);
            myInput.add(majorInput);

            // The last input is an extra input (action) that is only used by the first layer (rules layer)
            if ( layers.size() + 1 == input.size()) {
                OnnxTensor extraInput = input.get(input.size() - 1);
                myInput.add(0, extraInput);
            }

            OnnxBlock child = onnxIO.getOnnxBlock(counter, myInput);
            onnxBlock.addChild(child);
            if (child.getOutput().size() > 1)
                throw new RuntimeException("each output is assumed to be a single tensor here");
            childOutput = child.getOutput().get(0);
            outputsToBeConcatenated.add(childOutput);
            size += childOutput.getShape().get(concatDim);
            c++;
        }


        onnxBlock.setOutput(outputsToBeConcatenated);

        return onnxBlock;
    }


}
