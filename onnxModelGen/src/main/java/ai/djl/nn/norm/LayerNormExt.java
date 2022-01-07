package ai.djl.nn.norm;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.internal.NDArrayEx;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Parameter;
import ai.djl.nn.Parameter.Type;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.OnnxBlockExt;
import ai.enpasos.mnist.blocks.OnnxContext;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.convert;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class LayerNormExt extends AbstractBlock implements OnnxIO {
    private float epsilon;
    private Shape normalizedShape;
    private boolean center;
    private boolean scale;
    private int[] axis;
    private Parameter gamma;
    private Parameter beta;

    LayerNormExt(LayerNormExt.Builder builder) {
        this.epsilon = builder.epsilon;
        this.scale = builder.scale;
        this.center = builder.center;
        this.axis = builder.axis;
        this.gamma = this.addParameter(Parameter.builder().setName("gamma").setType(Type.GAMMA).optRequiresGrad(this.scale).build());
        this.beta = this.addParameter(Parameter.builder().setName("beta").setType(Type.BETA).optRequiresGrad(this.center).build());
    }

    public static NDList layerNorm(NDArray input, Shape normalizedShape, NDArray gamma, NDArray beta, float eps) {
        NDArrayEx ex = input.getNDArrayInternal();
        return ex.layerNorm(input, normalizedShape, gamma, beta, eps);
    }

    public static LayerNormExt.Builder builder() {
        return new LayerNormExt.Builder();
    }

    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {
        NDArray input = inputs.singletonOrThrow();
        Device device = input.getDevice();
        NDArray gammaArr = parameterStore.getValue(this.gamma, device, training);
        NDArray betaArr = parameterStore.getValue(this.beta, device, training);
        return layerNorm(input, this.normalizedShape, gammaArr, betaArr, this.epsilon);
    }

    public Shape[] getOutputShapes(Shape[] inputShapes) {
        return new Shape[]{inputShapes[0]};
    }

    protected void beforeInitialize(Shape... inputShapes) {
        super.beforeInitialize(inputShapes);
        this.normalizedShape = this.axis == null ? inputShapes[0].slice(1) : new Shape(Arrays.stream(this.axis).mapToLong((dim) -> {
            return inputShapes[0].get(dim);
        }).toArray());
    }

    public void prepare(Shape[] inputShapes) {
        this.gamma.setShape(this.normalizedShape);
        this.beta.setShape(this.normalizedShape);
    }

    protected void saveMetadata(DataOutputStream os) throws IOException {
        this.saveInputShapes(os);
        os.write(this.normalizedShape.getEncoded());
    }

    public void loadMetadata(byte loadVersion, DataInputStream is) throws IOException, MalformedModelException {
        if (loadVersion != this.version) {
            throw new MalformedModelException("Unsupported encoding version: " + loadVersion);
        } else {
            this.readInputShapes(is);
            this.normalizedShape = Shape.decode(is);
        }
    }

    @Override
    public OnnxBlockExt getOnnxBlockExt(OnnxContext ctx) {
        OnnxBlockExt onnxBlockExt = new OnnxBlockExt();

        Shape[] inputShapes = ctx.getInputShapes().toArray(new Shape[0]);
        String gammaName = "gamma" +  ctx.counter();
        String betaName = "beta" +  ctx.counter();
        String normalizationOutput = "LayerNormalizationOutput" +  ctx.counter();
        String mulOutput = "MulOutput" +  ctx.counter();
        String addOutput = "AddOutput" +  ctx.counter();

        onnxBlockExt.getNodes().add(
                nodeBuilderNormalization(
                        ctx.getInputNames().get(0),
                        normalizationOutput,
                        "MeanVarianceNormalization" +  ctx.counter()
                ).build());


        onnxBlockExt.getValueInfos().add(createValueInfoProto(normalizationOutput, inputShapes[0]));

        onnxBlockExt.getNodes().add(
                nodeBuilderMul(
                        normalizationOutput,
                        mulOutput,
                        "MulNode" +  ctx.counter(),
                        gammaName
                ).build());
        onnxBlockExt.getValueInfos().add(createValueInfoProto(mulOutput, inputShapes[0]));

        onnxBlockExt.getNodes().add(
                nodeBuilderAdd(
                        mulOutput,
                        addOutput,
                        "MulNode" +  ctx.counter(),
                        betaName
                ).build());
        onnxBlockExt.getValueInfos().add(createValueInfoProto(addOutput, inputShapes[0]));


        onnxBlockExt.setOutputShapes(List.of(this.getOutputShapes(inputShapes)));
        onnxBlockExt.getOutputNames().add( addOutput);

        NDArray gamma = this.parameters.get("gamma").getArray();
        onnxBlockExt.getParameters().add(
                TensorProto.newBuilder()
                .setName(gammaName)
                .setDataType(1)
                .addAllDims(convert(gamma.getShape().getShape()))
                .addAllFloatData(convert(gamma))
                .build());
        NDArray beta = this.parameters.get("beta").getArray();
        onnxBlockExt.getParameters().add(
                TensorProto.newBuilder()
                        .setName(betaName)
                        .setDataType(1)
                        .addAllDims(convert(beta.getShape().getShape()))
                        .addAllFloatData(convert(beta))
                        .build());



        return onnxBlockExt;
    }

//    private long[] transposeBatchChannelDims(long[] inputDims) {
//        long[] inputDims2 = Arrays.copyOf(inputDims, inputDims.length);
//        inputDims2[1] = inputDims[0];
//        inputDims2[0]= inputDims[1];
//        return inputDims2;
//    }

    private NodeProto.Builder nodeBuilderNormalization(String inputName, String outputName, String nodeName) {
        NodeProto.Builder nodeBuilder = NodeProto.newBuilder();
        nodeBuilder.setName(nodeName);
        nodeBuilder.setOpType("MeanVarianceNormalization");
        nodeBuilder.addAttribute(AttributeProto.newBuilder()
                .setType(AttributeProto.AttributeType.INTS)
                .setName("axes")
                .addAllInts(List.of(1L,2L,3L))
                .build());
        nodeBuilder.addInput(inputName);
        nodeBuilder.addOutput(outputName);
        return nodeBuilder;
    }
    private NodeProto.Builder nodeBuilderMul(String inputName, String outputName, String nodeName, String parameterName) {
        NodeProto.Builder nodeBuilder = NodeProto.newBuilder();
        nodeBuilder.setName(nodeName);
        nodeBuilder.setOpType("Mul");
        nodeBuilder.addInput(inputName);
        nodeBuilder.addInput(parameterName);
        nodeBuilder.addOutput(outputName);
        return nodeBuilder;
    }
    private NodeProto.Builder nodeBuilderAdd(String inputName, String outputName, String nodeName, String parameterName) {
        NodeProto.Builder nodeBuilder = NodeProto.newBuilder();
        nodeBuilder.setName(nodeName);
        nodeBuilder.setOpType("Add");
        nodeBuilder.addInput(inputName);
        nodeBuilder.addInput(parameterName);
        nodeBuilder.addOutput(outputName);
        return nodeBuilder;
    }

    public static final class Builder {
        private float epsilon = 1.0E-5F;
        private boolean scale = true;
        private boolean center = true;
        private int[] axis;

        Builder() {
        }

        public LayerNormExt.Builder axis(int... axis) {
            this.axis = axis;
            return this;
        }

        public LayerNormExt.Builder optCenter(boolean val) {
            this.center = val;
            return this;
        }

        public LayerNormExt.Builder optScale(boolean val) {
            this.scale = val;
            return this;
        }

        public LayerNormExt.Builder optEpsilon(float val) {
            this.epsilon = val;
            return this;
        }

        public LayerNormExt build() {
            return new LayerNormExt(this);
        }
    }
}
