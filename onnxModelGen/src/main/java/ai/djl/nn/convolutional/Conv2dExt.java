package ai.djl.nn.convolutional;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.LayoutType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.util.Preconditions;
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

public class Conv2dExt extends Convolution implements OnnxIO {
    private static final LayoutType[] EXPECTED_LAYOUT = {
            LayoutType.BATCH, LayoutType.CHANNEL, LayoutType.HEIGHT, LayoutType.WIDTH
    };
    private static final String STRING_LAYOUT = "NCHW";
    private static final int NUM_DIMENSIONS = 4;


    Conv2dExt(Builder builder) {
        super(builder);
    }

    Conv2dExt(Conv2d.Builder builder) {
        super(builder);
    }

    /**
     * Applies 2D convolution over an input signal composed of several input planes.
     *
     * @param input  the input {@code NDArray} of shape (batchSize, inputChannel, height, width)
     * @param weight filters {@code NDArray} of shape (outChannel, inputChannel/groups, height,
     *               width)
     * @return the output of the conv2d operation
     */
    public static NDList conv2d(NDArray input, NDArray weight) {
        return conv2d(input, weight, null, new Shape(1, 1), new Shape(0, 0), new Shape(1, 1));
    }

    /**
     * Applies 2D convolution over an input signal composed of several input planes.
     *
     * @param input  the input {@code NDArray} of shape (batchSize, inputChannel, height, width)
     * @param weight filters {@code NDArray} of shape (outChannel, inputChannel/groups, height,
     *               width)
     * @param bias   bias {@code NDArray} of shape (outChannel)
     * @return the output of the conv2d operation
     */
    public static NDList conv2d(NDArray input, NDArray weight, NDArray bias) {
        return conv2d(input, weight, bias, new Shape(1, 1), new Shape(0, 0), new Shape(1, 1));
    }

    /**
     * Applies 2D convolution over an input signal composed of several input planes.
     *
     * @param input  the input {@code NDArray} of shape (batchSize, inputChannel, height, width)
     * @param weight filters {@code NDArray} of shape (outChannel, inputChannel/groups, height,
     *               width)
     * @param bias   bias {@code NDArray} of shape (outChannel)
     * @param stride the stride of the convolving kernel: Shape(height, width)
     * @return the output of the conv2d operation
     */
    public static NDList conv2d(NDArray input, NDArray weight, NDArray bias, Shape stride) {
        return conv2d(input, weight, bias, stride, new Shape(0, 0), new Shape(1, 1));
    }

    /**
     * Applies 2D convolution over an input signal composed of several input planes.
     *
     * @param input   the input {@code NDArray} of shape (batchSize, inputChannel, height, width)
     * @param weight  filters {@code NDArray} of shape (outChannel, inputChannel/groups, height,
     *                width)
     * @param bias    bias {@code NDArray} of shape (outChannel)
     * @param stride  the stride of the convolving kernel: Shape(height, width)
     * @param padding implicit paddings on both sides of the input: Shape(height, width)
     * @return the output of the conv2d operation
     */
    public static NDList conv2d(
            NDArray input, NDArray weight, NDArray bias, Shape stride, Shape padding) {
        return conv2d(input, weight, bias, stride, padding, new Shape(1, 1));
    }

    /**
     * Applies 2D convolution over an input signal composed of several input planes.
     *
     * @param input    the input {@code NDArray} of shape (batchSize, inputChannel, height, width)
     * @param weight   filters {@code NDArray} of shape (outChannel, inputChannel/groups, height,
     *                 width)
     * @param bias     bias {@code NDArray} of shape (outChannel)
     * @param stride   the stride of the convolving kernel: Shape(height, width)
     * @param padding  implicit paddings on both sides of the input: Shape(height, width)
     * @param dilation the spacing between kernel elements: Shape(height, width)
     * @return the output of the conv2d operation
     */
    public static NDList conv2d(
            NDArray input,
            NDArray weight,
            NDArray bias,
            Shape stride,
            Shape padding,
            Shape dilation) {
        return conv2d(input, weight, bias, stride, padding, dilation, 1);
    }

    /**
     * Applies 2D convolution over an input signal composed of several input planes.
     *
     * @param input    the input {@code NDArray} of shape (batchSize, inputChannel, height, width)
     * @param weight   filters {@code NDArray} of shape (outChannel, inputChannel/groups, height,
     *                 width)
     * @param bias     bias {@code NDArray} of shape (outChannel)
     * @param stride   the stride of the convolving kernel: Shape(height, width)
     * @param padding  implicit paddings on both sides of the input: Shape(height, width)
     * @param dilation the spacing between kernel elements: Shape(height, width)
     * @param groups   split input into groups: input channel(input.size(1)) should be divisible by
     *                 the number of groups
     * @return the output of the conv2d operation
     */
    public static NDList conv2d(
            NDArray input,
            NDArray weight,
            NDArray bias,
            Shape stride,
            Shape padding,
            Shape dilation,
            int groups) {
        Preconditions.checkArgument(
                input.getShape().dimension() == 4 && weight.getShape().dimension() == 4,
                "the shape of input or weight doesn't match the conv2d");
        Preconditions.checkArgument(
                stride.dimension() == 2 && padding.dimension() == 2 && dilation.dimension() == 2,
                "the shape of stride or padding or dilation doesn't match the conv2d");
        return Convolution.convolution(input, weight, bias, stride, padding, dilation, groups);
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
        String parameter = "Parameter" +  ctx.counter();
        String convolutionOutput = "ConvolutionOutput" +  ctx.counter();
        onnxBlockExt.getNodes().add(
                nodeBuilder(
                        ctx.getInputNames().get(0),
                        convolutionOutput,
                        "ConvolutionNode" +  ctx.counter(),
                        parameter
                ).build());
        onnxBlockExt.setOutputShapes(List.of(this.getOutputShapes(ctx.getInputShapes().toArray(new Shape[0]))));
        onnxBlockExt.getValueInfos().add(createValueInfoProto(convolutionOutput, onnxBlockExt.getOutputShapes().get(0)));

        NDArray weights = this.parameters.get("weight").getArray();
        onnxBlockExt.getParameters().add(TensorProto.newBuilder()
                .setName(parameter)
                .setDataType(1)
                .addAllDims(convert(weights.getShape().getShape()))
                .addAllFloatData(convert(weights))
                .build());
        onnxBlockExt.getOutputNames().add(convolutionOutput);
        return onnxBlockExt;
    }

    private NodeProto.Builder nodeBuilder(String inputName, String outputName, String nodeName, String parameterName) {
        NodeProto.Builder nodeBuilder = NodeProto.newBuilder();
        nodeBuilder.setName(nodeName);
        nodeBuilder.setOpType("Conv");
        nodeBuilder.addAttribute(AttributeProto.newBuilder()
                .setType(AttributeProto.AttributeType.STRING)
                .setName("auto_pad")
                .setS(ByteString.copyFromUtf8("SAME_UPPER"))
                .build());
        nodeBuilder.addAttribute(AttributeProto.newBuilder()
                .setType(AttributeProto.AttributeType.INTS)
                .setName("dilations")
                .addAllInts(convert(this.dilation.getShape()))
                .build());
        nodeBuilder.addAttribute(AttributeProto.newBuilder()
                .setType(AttributeProto.AttributeType.INT)
                .setName("group")
                .setI(this.groups)
                .build());
        nodeBuilder.addAttribute(AttributeProto.newBuilder()
                .setType(AttributeProto.AttributeType.INTS)
                .setName("kernel_shape")
                .addAllInts(convert(this.kernelShape.getShape()))
                .build());
        nodeBuilder.addAttribute(AttributeProto.newBuilder()
                .setType(AttributeProto.AttributeType.INTS)
                .setName("strides")
                .addAllInts(convert(this.stride.getShape()))
                .build());
        nodeBuilder.addInput(inputName);
        nodeBuilder.addOutput(outputName);
        nodeBuilder.addInput(parameterName);
        return nodeBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LayoutType[] getExpectedLayout() {
        return EXPECTED_LAYOUT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getStringLayout() {
        return STRING_LAYOUT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int numDimensions() {
        return NUM_DIMENSIONS;
    }

    /**
     * The Builder to construct a {@link Conv2d} type of {@link Block}.
     */
    public static final class Builder extends ConvolutionBuilder<Conv2dExt.Builder> {

        /**
         * Creates a builder that can build a {@link Conv2d} block.
         */
        Builder() {
            stride = new Shape(1, 1);
            padding = new Shape(0, 0);
            dilation = new Shape(1, 1);
        }

        /**
         * {@inheritDoc}
         */
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
