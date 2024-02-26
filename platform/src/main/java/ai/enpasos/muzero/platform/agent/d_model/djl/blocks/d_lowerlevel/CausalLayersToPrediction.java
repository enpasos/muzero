package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
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
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.*;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

@SuppressWarnings("all")
public class CausalLayersToPrediction extends AbstractBlock implements OnnxIO  {


    public CausalLayersToPrediction(  ) {

    }


    // The inputs are the major inputs to the layers
    // They are ordered the same way as the layers (bottom up)
    // The outputs are the outputs of the layers again ordered bottom up
    // The inputs to the layers are ordered again bottom up (the major one is the last one)
    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {
        NDList combinedResult  = new NDList();
        for(int k = 0; k < inputs.size(); k++) {
            NDList layerInput = new NDList();
            for(int j = 0; j < k; j++) {
                // There is no backpropagation across layer boundaries
                NDArray minorInput = inputs.get(j).stopGradient();
                layerInput.add(minorInput);
            }
            NDArray majorInput = inputs.get(k);
            layerInput.add(majorInput);
            NDArray result = NDArrays.concat(layerInput,1);
            combinedResult.add(result);
        }
        return combinedResult;
    }



    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {

        long[] concats = new long[inputShapes.length];
        for(int k = 0; k < inputShapes.length; k++) {
            // List<Shape> layerInputShapes = new ArrayList<>();
            for(int j = 0; j <= k; j++) {
                concats[k] += inputShapes[j].get(1);
            }
        }
        Shape[] outputShapes = new Shape[  inputShapes.length];
        for(int k = 0; k < inputShapes.length; k++) {
             long[] shape = inputShapes[k].getShape();
             shape = shape.clone();
             shape[1] = concats[k];
                outputShapes[k] = new Shape(shape);
        }
        return outputShapes;
    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {

    }




    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {

        Shape[] inputShapes = getShapes(input).toArray(new Shape[0]);
        Shape[] outputShapes = getOutputShapes( inputShapes);

        List<List<OnnxTensor>> metaInputs = new ArrayList<>();
        for(int k = 0; k < input.size(); k++) {
            List<OnnxTensor> layerInput = new ArrayList<>();
            for(int j = 0; j <= k; j++) {
                OnnxTensor inputA = input.get(j);
                layerInput.add(inputA);
            }
            metaInputs.add(layerInput);
        }
        List<OnnxTensor> output = new ArrayList<>();

        OnnxBlock onnxBlock = OnnxBlock.builder()
                .input(input)
                .valueInfos(createValueInfoProto(input))
                .build();

        int concatDim = 1;

        for(int k = 0; k < metaInputs.size(); k++) {
            List<OnnxTensor> input2 = metaInputs.get(k);
            List<OnnxTensor> output2 = combine(
                    List.of("T" + counter.count()),
                    List.of(outputShapes[k])
            );
            onnxBlock.getNodes().add(
                    getConcatNode(counter, input2, concatDim, output2)
            );
            onnxBlock.getValueInfos().addAll(createValueInfoProto(output));
            output.add(output2.get(0));
        }
        onnxBlock.setOutput(output);
        onnxBlock.getValueInfos().addAll(createValueInfoProto(output));

        return onnxBlock;
    }

    @NotNull
    private static NodeProto getConcatNode(OnnxCounter counter, List<OnnxTensor> input, int concatDim, List<OnnxTensor> output) {


        return NodeProto.newBuilder()
                .setName("N" + counter.count())
                .setOpType("Concat")
                .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INT)
                        .setName("axis")
                        .setI(concatDim)
                        .build())
                .addAllInput(getNames(input))
                .addOutput(output.get(0).getName())
                .build();
    }



}
