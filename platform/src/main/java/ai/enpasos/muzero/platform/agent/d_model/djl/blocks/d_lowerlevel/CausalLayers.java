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
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.StoringOnOff;
import ai.enpasos.muzero.platform.common.MuZeroException;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

@SuppressWarnings("all")
public class CausalLayers extends AbstractBlock implements OnnxIO, DCLAware {

    private final List<Block> layers;
    private final boolean rescale;


    @Override
    public void freezeParameters(boolean[] freeze) {
        for(int i = 0; i < freeze.length; i++) {
            layers.get(i).freezeParameters(freeze[i]);
        }
    }

    @Override
    public void setExportFilter(boolean[] exportFilter) {
        for(int i = 0; i < exportFilter.length; i++) {
            ((StoringOnOff) layers.get(i)).setStoring(exportFilter[i]);
        }
    }


    // Layers are ordered bottom up in the list
    // Each layer may depend on the output of the previous layers (with s)
    // There is no backpropagation across layer boundaries
    public CausalLayers(List<Block> layers, boolean rescale ) {
        this.layers = layers;
        this.rescale = rescale;
        for(Block layer: layers) {
            this.addChildBlock("cl", layer);
        }

    }


    // The inputs are the major inputs to the layers
    // They are ordered the same way as the layers (bottom up)
    // The outputs are the outputs of the layers again ordered bottom up
    // The inputs to the layers are ordered again bottom up (the major one is the last one)
    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {
        NDList combinedResultA = new NDList();
        NDList combinedResultB = new NDList();
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

            // The last input is an extra input (action)
            if ( layers.size() + 1 == inputs.size()) {
                NDArray extraInput = inputs.get(inputs.size() - 1);
                layerInput.add(0, extraInput);
            }

            NDList resultList = layer.forward(parameterStore, layerInput, training, params);
            combinedResultA.add(resultList.get(0));  // for prediction
//            if (rescale) {
//                combinedResultB.add(resultList.get(1));  // for time evolution
//            }
        }
        return combinedResultA.addAll(combinedResultB);
    }



    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
      //  Shape[] outputShapes = new Shape[rescale ? 2*layers.size() : layers.size()];
        Shape[] outputShapes = new Shape[  layers.size()];
        for (int i = 0; i < layers.size(); i++) {
            Shape[] outputShapesHere = layers.get(i).getOutputShapes(new Shape[] {inputShapes[i]});
            outputShapes[i] = outputShapesHere[0];
//            if (rescale) {
//                outputShapes[i + layers.size()] = outputShapesHere[1];
//            }
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
        List<OnnxTensor> outputsA = new ArrayList<>();
        List<OnnxTensor> outputsB = new ArrayList<>();
        OnnxTensor childOutputA = null;
        OnnxTensor childOutputB = null;
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
            childOutputA = child.getOutput().get(0);
            outputsA.add(childOutputA);
//            if (rescale) {
//                childOutputB = child.getOutput().get(1);
//                outputsB.add(childOutputB);
//            }
            c++;
        }
//        if (rescale) {
//            outputsA.addAll(outputsB);
//        }
        onnxBlock.setOutput(outputsA);

        return onnxBlock;
    }


}
