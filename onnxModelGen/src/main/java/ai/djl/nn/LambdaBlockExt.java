package ai.djl.nn;

import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.OnnxBlockExt;
import ai.enpasos.mnist.blocks.OnnxContext;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;
import com.google.protobuf.ByteString;
import org.apache.commons.lang3.NotImplementedException;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

/**
 * {@code LambdaBlock} is a {@link Block} with no parameters or children.
 *
 * <p>{@code LambdaBlock} allows converting any function that takes an {@code NDList} as input and
 * returns an {@code NDList} into a parameter-less and child-less {@link Block}. This can be useful
 * in converting activation functions, identity blocks, and more. For example, identity block can be
 * created as {@code new LambdaBlock(x -> x)}.
 */
public class LambdaBlockExt extends AbstractBlock implements OnnxIO {

    private static final byte VERSION = 2;
    private final Type type;
    private Function<NDList, NDList> lambda;

    /**
     * Creates a LambdaBlock that can apply the specified function.
     *
     * @param lambda the function to apply
     */
    public LambdaBlockExt(Type type, Function<NDList, NDList> lambda) {
        super(VERSION);
        this.type = type;
        this.lambda = lambda;
    }

    /**
     * Creates a {@link LambdaBlockExt} for a singleton function.
     *
     * @param lambda a function accepting a singleton {@link NDList} and returning another sinleton
     *               {@link NDList}
     * @return a new {@link LambdaBlockExt} for the function
     */
    public static LambdaBlockExt singleton(Type type, Function<NDArray, NDArray> lambda) {
        return new LambdaBlockExt(type, arrays -> new NDList(lambda.apply(arrays.singletonOrThrow())));
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
        return lambda.apply(inputs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDList input = new NDList(inputShapes.length);
            for (Shape shape : inputShapes) {
                input.add(manager.zeros(shape));
            }
            NDList output = lambda.apply(input);
            Shape[] outputShapes = new Shape[output.size()];
            for (int i = 0; i < output.size(); ++i) {
                outputShapes[i] = output.get(i).getShape();
            }
            return outputShapes;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadParameters(NDManager manager, DataInputStream is)
            throws IOException, MalformedModelException {
        byte version = is.readByte();
        if (version == VERSION) {
            readInputShapes(is);
        } else if (version != 1) {
            throw new MalformedModelException("Unsupported encoding version: " + version);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Lambda()";
    }

    @Override
    public OnnxBlockExt getOnnxBlockExt(OnnxContext ctx) {
        OnnxBlockExt onnxBlockExt = new OnnxBlockExt();
        String outputName = "lambdaBlockOutput" + ctx.counter();
        onnxBlockExt.getNodes().add(nodeBuilder(ctx, onnxBlockExt, ctx.getInputNames().get(0), outputName).build());
        onnxBlockExt.setOutputShapes(Arrays.asList(this.getOutputShapes(ctx.getInputShapes().toArray(new Shape[0]))));
        onnxBlockExt.getOutputNames().add(outputName);
        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputName,onnxBlockExt.getOutputShapes().get(0)));
        return onnxBlockExt;
    }

    private NodeProto.Builder nodeBuilder(OnnxContext ctx, OnnxBlockExt onnxBlockExt, String inputName, String outputName) {
        NodeProto.Builder nodeBuilder = NodeProto.newBuilder();
        nodeBuilder.addInput(inputName);

        switch (this.type) {
            case MAX_POOLING:
                nodeBuilder.setName("maxPoolNode" + ctx.counter());
                nodeBuilder.setOpType("MaxPool");
                nodeBuilder.addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.STRING)
                        .setName("auto_pad")
                        .setS(ByteString.copyFromUtf8("NOTSET"))
                        .build());
                nodeBuilder.addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("kernel_shape")
                        .addAllInts(List.of(2L, 2L))
                        .build());
                 nodeBuilder.addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("strides")
                        .addAllInts(List.of(2L, 2L))
                        .build());
                break;
            case BATCH_FLATTEN:
                nodeBuilder.setName("batchFlattenNode" + ctx.counter());
                nodeBuilder.setOpType("Reshape");
                String shapeName = "batchFlattenNodeShape" + ctx.counter();
                long size = ctx.getInputShapes().get(0).size();
                onnxBlockExt.getParameters().add(TensorProto.newBuilder()
                        .setName(shapeName)
                        .setDataType(TensorProto.INT64_DATA_FIELD_NUMBER)
                        .addAllDims(List.of(2L))
                        .addAllInt64Data(List.of(1L, size))
                        .build());
                nodeBuilder.addInput(shapeName);
                break;
            case RELU:
            case IDENTITY:
            case NOT_IMPLEMENTED_YET:
            default:
                throw new NotImplementedException(type.name());
        }

        nodeBuilder.addOutput(outputName);
        return nodeBuilder;
    }

    public enum Type {IDENTITY, RELU, MAX_POOLING, BATCH_FLATTEN, NOT_IMPLEMENTED_YET}
}
