package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.nn.ParallelBlock;
import ai.djl.training.ParameterStore;
import ai.djl.util.Pair;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.RewardBlock;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;
import static ai.enpasos.mnist.blocks.OnnxBlock.getNames;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

@SuppressWarnings("all")
public class CausalLayers extends AbstractBlock implements OnnxIO {

    private final List<Block> layers;


    // Layers are ordered bottom up in the list
    // Each layer may depend on the output of the previous layers (with s)
    // There is no backpropagation across layer boundaries
    public CausalLayers(List<Block> layers ) {
        this.layers = layers;
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
                NDArray minorInput = combinedResult.get(j).stopGradient();
                layerInput.add(minorInput);
            }
            NDArray majorInput = inputs.get(k);
            layerInput.add(majorInput);

            // The last input is an extra input (action) that is only used by the first layer (rules layer)
            if (k == 0 && layers.size() + 1 == inputs.size()) {
                NDArray extraInput = inputs.get(inputs.size() - 1);
                layerInput.add(extraInput);
            }

            NDArray resultArray = layer.forward(parameterStore, layerInput, training, params).get(0);
            combinedResult.add(resultArray);
        }
        return combinedResult;
    }


    // The output format is exactly the same as the input format
    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        return inputShapes;
    }



    // TODO t.b.d.
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
        for (Pair<String, Block> p : this.getChildren()) {
            OnnxIO onnxIO = (OnnxIO) p.getValue();
            OnnxBlock child = onnxIO.getOnnxBlock(counter, input);
            onnxBlock.addChild(child);
            if (child.getOutput().size() > 1)
                throw new RuntimeException("each output is assumed to be a single tensor here");
            childOutput = child.getOutput().get(0);
            outputsToBeConcatenated.add(childOutput);
            size += childOutput.getShape().get(concatDim);
        }

        Shape inputShapeExample = childOutput.getShape();


        List<OnnxTensor> output = null;
        if (inputShapeExample.dimension() == 4) {
            output = combine(List.of("T" + counter.count()), List.of(
                new Shape(inputShapeExample.get(0), size, inputShapeExample.get(2), inputShapeExample.get(3))
            ));
        } else {
            output = combine(List.of("T" + counter.count()), List.of(
                new Shape(inputShapeExample.get(0), size)
            ));
        }

        OnnxBlock concatBlock = OnnxBlock.builder()
            .input(outputsToBeConcatenated)
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("Concat")
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INT)
                        .setName("axis")
                        .setI(concatDim)
                        .build())
                    .addAllInput(getNames(outputsToBeConcatenated))
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .valueInfos(createValueInfoProto(output))
            .build();

        onnxBlock.addChild(concatBlock);
        onnxBlock.setOutput(concatBlock.getOutput());

        return onnxBlock;
    }


}
