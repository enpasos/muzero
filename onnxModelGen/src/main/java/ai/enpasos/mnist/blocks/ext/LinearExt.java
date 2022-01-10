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

        List<Shape> outputShapes = List.of(this.getOutputShapes(ctx.getInputShapes().toArray(new Shape[0])));
        long inputDim = ctx.getInputShapes().get(0).get(1);
        long outputDim = outputShapes.get(0).get(1);


        String outputWName = "LinearWOutput" + ctx.counter();
        nodeW(ctx, onnxBlockExt, outputWName, new Shape(new long[]{inputDim, outputDim}));

        String outputMulName = "MultOutput" + ctx.counter();
        nodeMult(ctx, onnxBlockExt, outputWName, ctx.getInputNames().get(0), outputMulName, outputShapes.get(0));

        String outputAddName = "AddOutput" + ctx.counter();
        nodeB(ctx, onnxBlockExt, outputMulName,  outputAddName, outputShapes.get(0));

        onnxBlockExt.getOutputNames().add(outputAddName);
        onnxBlockExt.setOutputShapes(outputShapes);
        return onnxBlockExt;
    }

    private void nodeB(OnnxContext ctx, OnnxBlockExt onnxBlockExt, String input, String outputAddName, Shape newShape ) {
        String parameterName = "Parameter" + ctx.counter();
        onnxBlockExt.getNodes().add(NodeProto.newBuilder()
                .setName("Node" + ctx.counter())
                .setOpType("Add")
                .addInput(input)
                .addInput(parameterName)
                .addOutput(outputAddName)
                .build());
        onnxBlockExt.getParameters().add(TensorProto.newBuilder()
                .setName(parameterName)
                .setDataType(1)
                .addAllDims(List.of(1L, 10L))
                .addAllFloatData(convert(this.parameters.get("bias").getArray()))
                .build());

        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputAddName, newShape));
    }

    private  void nodeMult(OnnxContext ctx, OnnxBlockExt onnxBlockExt, String inputA, String inputB, String output,  Shape newShape) {
        onnxBlockExt.getNodes().add(NodeProto.newBuilder()
                .setName("Mul" + ctx.counter())
                .setOpType("MatMul")
                .addInput(inputB)
                .addInput(inputA)
                .addOutput(output)
                .build());
        onnxBlockExt.getValueInfos().add(createValueInfoProto(output, newShape));
    }

    private void nodeW(OnnxContext ctx, OnnxBlockExt onnxBlockExt, String outputName, Shape shape) {
        String shapeName = "batchFlattenNodeShape" + ctx.counter();
        String parameterName = "Parameter"+ ctx.counter();

        onnxBlockExt.getNodes().add(NodeProto.newBuilder()
                .setName("Node" + ctx.counter())
                .setOpType("Reshape")
                .addInput(parameterName)
                .addInput(shapeName)
                .addOutput(outputName)
                .build());

        NDArray weight = this.parameters.get("weight").getArray().transpose();
        onnxBlockExt.getParameters().add(TensorProto.newBuilder()
                .setName(parameterName)
                .setDataType(1)
                .addAllDims(convert(weight.getShape().getShape()))
                .addAllFloatData(convert(weight))
                .build());

        long size = ctx.getInputShapes().get(0).size();
        onnxBlockExt.getParameters().add(TensorProto.newBuilder()
                .setName(shapeName)
                .setDataType(TensorProto.INT64_DATA_FIELD_NUMBER)
                .addAllDims(List.of(2L))
                .addAllInt64Data(List.of(size, 10L))
                .build());

        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputName, shape ));
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
