package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.nn.LambdaBlockOpened;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;
import com.google.protobuf.ByteString;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.function.Function;

import static ai.enpasos.mnist.blocks.OnnxBlock.createOutput;
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
    public OnnxBlock getOnnxBlock(OnnxCounter ctx, List<OnnxTensor> input) {

        String outputName = "T" + ctx.count();
        List<OnnxTensor> output = createOutput(List.of(outputName), input, this::getOutputShapes);
        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .build();

        NodeProto.Builder nodeBuilder = NodeProto.newBuilder()
            .addInput(input.get(0).getName())
            .addOutput(outputName)
            .setName("N" + ctx.count());

        switch (this.type) {
            case MAX_POOLING:
                nodeBuilder
                    .setOpType("MaxPool")
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.STRING)
                        .setName("auto_pad")
                        .setS(ByteString.copyFromUtf8("NOTSET"))
                        .build())
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("kernel_shape")
                        .addAllInts(List.of(2L, 2L))
                        .build())
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("strides")
                        .addAllInts(List.of(2L, 2L))
                        .build());

                break;
            case BATCH_FLATTEN:
                String shapeName = "batchFlattenNodeShape" + ctx.count();
                nodeBuilder.setOpType("Reshape")
                    .addInput(shapeName);
                long size = input.get(0).getShape().size();
                onnxBlock.getParameters().add(TensorProto.newBuilder()
                    .setName(shapeName)
                    .setDataType(TensorProto.INT64_DATA_FIELD_NUMBER)
                    .addAllDims(List.of(2L))
                    .addAllInt64Data(List.of(1L, size))
                    .build());
                break;
            case RELU:
                nodeBuilder.setOpType("Relu");
                break;
            case IDENTITY:
            case NOT_IMPLEMENTED_YET:
            default:
                throw new NotImplementedException(type.name());
        }
        onnxBlock.getNodes().add(nodeBuilder.build());

        return onnxBlock;
    }

    public enum Type {IDENTITY, RELU, MAX_POOLING, BATCH_FLATTEN, NOT_IMPLEMENTED_YET}
}
