package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.core.Linear;
import ai.djl.nn.core.LinearOpened;
import ai.djl.util.Preconditions;
import ai.enpasos.mnist.blocks.OnnxBlockExt;
import ai.enpasos.mnist.blocks.OnnxContext;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;

import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.convert;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class LinearExt extends LinearOpened implements OnnxIO {

    String parameterW;
    String parameterB;

    private String outputWName;
    private String nodeWName;
    private String outputBName;
    private String nodeBName;
    private String outputMulName;
    private String nodeMulName;
    private String outputAddName;
    private String nodeAddName;

    LinearExt(Builder builder) {
        super(builder);
    }

    /**
     * Creates a builder to build a {@code Linear}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new  Builder();
    }

    @Override
    public OnnxBlockExt getOnnxBlockExt(OnnxContext ctx) {
        OnnxBlockExt onnxBlockExt = new OnnxBlockExt();
        outputWName = "LinearW_Output_" + ctx.counter();
        nodeWName = "LinearW_Node_" + ctx.counter();
        parameterW = "Parameter" + ctx.counter();
        parameterB = "Parameter" + ctx.counter();

        nodeMulName = "Mult_" + ctx.counter();
        outputMulName = "Mult_Output_" + ctx.counter();
        nodeAddName = "Add_" + ctx.counter();
        outputAddName = "Add_Output_" + ctx.counter();

        onnxBlockExt.getNodes().add(nodeBuilderW(ctx, onnxBlockExt, outputWName, parameterW).build());

        List<Shape> outputShapes = List.of(this.getOutputShapes(ctx.getInputShapes().toArray(new Shape[0])));
        long inputDim = ctx.getInputShapes().get(0).get(1);
        long outputDim = outputShapes.get(0).get(1);

        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputWName, new Shape(new long[]{inputDim, outputDim})));

        NodeProto nodeMult = nodeBuilderMult(ctx, onnxBlockExt, outputWName, ctx.getInputNames().get(0)).build();
        onnxBlockExt.getNodes().add(nodeMult);
        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputMulName, new Shape(new long[]{1, outputDim})));

        NodeProto nodeAdd = nodeBuilderAdd(ctx, onnxBlockExt, nodeMult.getOutputList().get(0), parameterB).build();
        onnxBlockExt.getNodes().add(nodeAdd);

        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputAddName, new Shape(new long[]{1, outputDim})));

        onnxBlockExt.getOutputNames().add(outputAddName);
        onnxBlockExt.setOutputShapes(outputShapes);
        return onnxBlockExt;
    }

    private NodeProto.Builder nodeBuilderAdd(OnnxContext ctx, OnnxBlockExt onnxBlockExt, String input, String parameterBName) {
        NodeProto.Builder nodeBuilder = NodeProto.newBuilder();
        nodeBuilder.setName(nodeAddName);
        nodeBuilder.setOpType("Add");
        nodeBuilder.addInput(input);
        NDArray bias = this.parameters.get("bias").getArray();
        onnxBlockExt.getParameters().add(TensorProto.newBuilder()
                .setName(parameterBName)
                .setDataType(1)
                .addAllDims(List.of(1L, 10L))
                .addAllFloatData(convert(bias))
                .build());
        nodeBuilder.addInput(parameterBName);
        nodeBuilder.addOutput(outputAddName);
        return nodeBuilder;
    }

    private NodeProto.Builder nodeBuilderMult(OnnxContext ctx, OnnxBlockExt onnxBlockExt, String inputA, String inputB) {
        NodeProto.Builder nodeBuilder = NodeProto.newBuilder();
        nodeBuilder.setName(nodeMulName);
        nodeBuilder.setOpType("MatMul");
        nodeBuilder.addInput(inputB);
        nodeBuilder.addInput(inputA);
        nodeBuilder.addOutput(outputMulName);
        return nodeBuilder;
    }

    private NodeProto.Builder nodeBuilderW(OnnxContext ctx, OnnxBlockExt onnxBlockExt, String outputName, String parameterName) {
        NodeProto.Builder nodeBuilder = NodeProto.newBuilder();
        nodeBuilder.setName(nodeWName);
        nodeBuilder.setOpType("Reshape");

        NDArray weights = this.parameters.get("weight").getArray();
        NDArray weights2 = weights.transpose();
        onnxBlockExt.getParameters().add(TensorProto.newBuilder()
                .setName(parameterName)
                .setDataType(1)
                .addAllDims(convert(weights2.getShape().getShape()))
                .addAllFloatData(convert(weights2))
                .build());

        String shapeName = "batchFlattenNodeShape" + ctx.counter();
        long size = ctx.getInputShapes().get(0).size();
        onnxBlockExt.getParameters().add(TensorProto.newBuilder()
                .setName(shapeName)
                .setDataType(TensorProto.INT64_DATA_FIELD_NUMBER)
                .addAllDims(List.of(2L))
                .addAllInt64Data(List.of(size, 10L))
                .build());

        nodeBuilder.addInput(parameterW);
        nodeBuilder.addInput(shapeName);

        nodeBuilder.addOutput(outputName);
        return nodeBuilder;
    }


    /**
     * The Builder to construct a {@link Linear} type of {@link Block}.
     */
    public static final class Builder extends LinearOpened.Builder {

        Builder() {
            super();
        }


        /**
         * Returns the constructed {@code Linear}.
         *
         * @return the constructed {@code Linear}
         * @throws IllegalArgumentException if all required parameters (outChannels) have not been
         *                                  set
         */
        public LinearExt build() {
            Preconditions.checkArgument(units > 0, "You must specify unit");
            return new LinearExt(this);
        }
    }
}
