package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.convolutional.Conv2dOpened;
import ai.enpasos.mnist.blocks.OnnxBlockExt;
import ai.enpasos.mnist.blocks.OnnxContext;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;
import com.google.protobuf.ByteString;

import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.convert;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class Conv2dExt extends Conv2dOpened implements OnnxIO {

    Conv2dExt(Builder builder) {
        super(builder);
    }

    /**
     * Creates a builder to build a {@code Conv2d}.
     *
     * @return a new builder
     */
    public static Conv2dExt.Builder builder() {
        return new Conv2dExt.Builder();
    }

    @Override
    public OnnxBlockExt getOnnxBlockExt(OnnxContext ctx) {
        OnnxBlockExt onnxBlockExt = new OnnxBlockExt();

        List<Shape> outputShapes = List.of(this.getOutputShapes(ctx.getInputShapes().toArray(new Shape[0])));

        String parameter = "Parameter" +  ctx.counter();
        String convolutionOutput = "ConvolutionOutput" +  ctx.counter();



        onnxBlockExt.getNodes().add(
                nodeBuilder(
                        ctx.getInputNames().get(0),
                        convolutionOutput,
                        "ConvolutionNode" +  ctx.counter(),
                        parameter
                ).build());
        onnxBlockExt.getValueInfos().add(createValueInfoProto(convolutionOutput, outputShapes.get(0)));

        NDArray weights = this.parameters.get("weight").getArray();
        onnxBlockExt.getParameters().add(TensorProto.newBuilder()
                .setName(parameter)
                .setDataType(1)
                .addAllDims(convert(weights.getShape().getShape()))
                .addAllFloatData(convert(weights))
                .build());

        onnxBlockExt.setOutputShapes(outputShapes);
        onnxBlockExt.getOutputNames().add(convolutionOutput);
        return onnxBlockExt;
    }

    private NodeProto.Builder nodeBuilder(String inputName, String outputName, String nodeName, String parameterName) {
        return NodeProto.newBuilder()
                .setName(nodeName)
                .setOpType("Conv")
                .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.STRING)
                        .setName("auto_pad")
                        .setS(ByteString.copyFromUtf8("SAME_UPPER"))
                        .build())
                .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("dilations")
                        .addAllInts(convert(this.dilation.getShape()))
                        .build())
                .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INT)
                        .setName("group")
                        .setI(this.groups)
                        .build())
                .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("kernel_shape")
                        .addAllInts(convert(this.kernelShape.getShape()))
                        .build())
                .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("strides")
                        .addAllInts(convert(this.stride.getShape()))
                        .build())
                .addInput(inputName)
                .addInput(parameterName)
                .addOutput(outputName);
    }

    /**
     * The Builder to construct a {@link Conv2d} type of {@link Block}.
     */
    public static final class Builder extends Conv2dOpened.Builder {

        /**
         * Creates a builder that can build a {@link Conv2d} block.
         */
        Builder() {
            super();
            stride = new Shape(1, 1);
            padding = new Shape(0, 0);
            dilation = new Shape(1, 1);
        }

        @Override
        protected Conv2dExt.Builder self() {
            return this;
        }

        /**
         * Builds a {@link Conv2d} block.
         *
         * @return the {@link Conv2d} block
         */
        public Conv2dExt build() {
            validate();
            return new Conv2dExt(this);
        }
    }
}
