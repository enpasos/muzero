package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.LambdaBlockOpened;
import ai.enpasos.mnist.blocks.OnnxBlockExt;
import ai.enpasos.mnist.blocks.OnnxContext;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;
import com.google.protobuf.ByteString;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;


public class LambdaBlockExt extends LambdaBlockOpened implements OnnxIO {

    private final Type type;

    public LambdaBlockExt(Type type, Function<NDList, NDList> lambda) {
        super(lambda);
        this.type = type;
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
        NodeProto.Builder nodeBuilder = NodeProto.newBuilder()
            .addInput(inputName)
            .addOutput(outputName);
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

        return nodeBuilder;
    }

    public enum Type {IDENTITY, RELU, MAX_POOLING, BATCH_FLATTEN, NOT_IMPLEMENTED_YET}
}
