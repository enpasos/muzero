package ai.djl.nn.core;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.nn.Parameter;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.djl.util.Preconditions;
import ai.enpasos.mnist.blocks.OnnxBlockExt;
import ai.enpasos.mnist.blocks.OnnxContext;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.convert;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

/**
 * A Linear block applies a linear transformation \(Y = XW^T + b\).
 *
 * <p>It has the following shapes:
 *
 * <ul>
 *   <li>input X: [x1, x2, …, xn, input_dim]
 *   <li>weight W: [units, input_dim]
 *   <li>Bias b: [units]
 *   <li>outputWName Y: [x1, x2, …, xn, units]
 * </ul>
 *
 * <p>The Linear block should be constructed using {@link Linear.Builder}.
 */
public class LinearExt extends AbstractBlock implements OnnxIO {

    private static final byte VERSION = 4;
    String parameterW;
    String parameterB;
    private long units;
    private long inputFeatures;
    private Shape inputShape;
    private Parameter weight;
    private Parameter bias;
    private String outputWName;
    private String nodeWName;
    private String outputBName;
    private String nodeBName;
    private String outputMulName;
    private String nodeMulName;
    private String outputAddName;
    private String nodeAddName;

    LinearExt(Builder builder) {
        super(VERSION);
        units = builder.units;
        weight =
                addParameter(
                        Parameter.builder()
                                .setName("weight")
                                .setType(Parameter.Type.WEIGHT)
                                .build());
        if (builder.bias) {
            bias =
                    addParameter(
                            Parameter.builder()
                                    .setName("bias")
                                    .setType(Parameter.Type.BIAS)
                                    .build());
        }
    }

    /**
     * Applies a linear transformation to the incoming data.
     *
     * @param input  input X: [x1, x2, …, xn, input_dim]
     * @param weight weight W: [units, input_dim]
     * @return outputWName Y: [x1, x2, …, xn, units]
     */
    public static NDList linear(NDArray input, NDArray weight) {
        return linear(input, weight, null);
    }

    /**
     * Applies a linear transformation to the incoming data.
     *
     * @param input  input X: [x1, x2, …, xn, input_dim]
     * @param weight weight W: [units, input_dim]
     * @param bias   bias b: [units]
     * @return outputWName Y: [x1, x2, …, xn, units]
     */
    public static NDList linear(NDArray input, NDArray weight, NDArray bias) {
        return input.getNDArrayInternal().linear(input, weight, bias);
    }

    /**
     * Creates a builder to build a {@code Linear}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NDList forwardInternal(
            ParameterStore parameterStore,
            NDList inputs,
            boolean training,
            PairList<String, Object> params) {
        NDArray input = inputs.singletonOrThrow();
        Device device = input.getDevice();
        NDArray weightArr = parameterStore.getValue(weight, device, training);
        NDArray biasArr = parameterStore.getValue(bias, device, training);
        return linear(input, weightArr, biasArr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        return new Shape[]{inputs[0].slice(0, inputs[0].dimension() - 1).add(units)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PairList<String, Shape> describeInput() {
        return new PairList<>(
                Collections.singletonList("linearInput"), Collections.singletonList(inputShape));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeInitialize(Shape... inputShapes) {
        super.beforeInitialize(inputShapes);
        Preconditions.checkArgument(inputShapes.length == 1, "Linear block only support 1 input");
        Shape input = inputShapes[0];
        inputFeatures = input.get(input.dimension() - 1);
        inputShape = input.slice(0, input.dimension() - 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepare(Shape[] inputShapes) {
        Shape input = inputShapes[0];
        weight.setShape(new Shape(units, input.get(input.dimension() - 1)));
        if (bias != null) {
            bias.setShape(new Shape(units));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveMetadata(DataOutputStream os) throws IOException {
        os.writeLong(units);
        os.writeLong(inputFeatures);
        os.write(inputShape.getEncoded());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadMetadata(byte loadVersion, DataInputStream is)
            throws IOException, MalformedModelException {
        switch (loadVersion) {
            case VERSION:
                units = is.readLong();
                inputFeatures = is.readLong();
                break;
            case 3:
                units = is.readLong();
                if (is.readBoolean()) {
                    throw new IllegalArgumentException("flatten is not supported!");
                }
                inputFeatures = is.readLong();
                break;
            case 2:
                if (is.readBoolean()) {
                    throw new IllegalArgumentException("flatten is not supported!");
                }
                inputFeatures = is.readLong();
                break;
            case 1:
                inputFeatures = Shape.decode(is).size();
                break;
            default:
                throw new MalformedModelException("Unsupported encoding version: " + loadVersion);
        }
        inputShape = Shape.decode(is);
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
    public static final class Builder {

        private long units;
        private boolean bias = true;

        Builder() {
        }

        /**
         * Sets the number of outputWName channels.
         *
         * @param units the number of desired outputWName channels
         * @return this Builder
         */
        public Builder setUnits(long units) {
            this.units = units;
            return this;
        }

        /**
         * Sets the optional parameter that indicates whether to include a bias vector with default
         * value of true.
         *
         * @param bias whether to use a bias vector parameter
         * @return this Builder
         */
        public Builder optBias(boolean bias) {
            this.bias = bias;
            return this;
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
