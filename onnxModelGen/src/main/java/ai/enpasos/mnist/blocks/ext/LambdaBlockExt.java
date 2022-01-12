package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
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

import static ai.enpasos.mnist.blocks.OnnxBlock.*;
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

    private void addOutputToOnnxBlockAsInDJL(OnnxBlock onnxBlock,List<OnnxTensor> input, String outputName) {
        List<OnnxTensor> output = createOutput(List.of(outputName), input, this::getOutputShapes);
         onnxBlock.setOutput(output);
         onnxBlock.getValueInfos().add(createValueInfoProto(output).get(0));
    }
    private void addOutputToOnnxBlockAddingMissingDimensions(OnnxBlock onnxBlock,List<OnnxTensor> input, String outputName) {
        Shape outputShape =   getOutputShapes(getShapes(input).toArray(new Shape[0]))[0];
        List<OnnxTensor> output = combine(List.of(outputName),List.of(new Shape(outputShape.get(0), outputShape.get(1), (long)1, (long)1)));
        onnxBlock.setOutput(output);
        onnxBlock.getValueInfos().add(createValueInfoProto(output).get(0));
    }

    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter ctx, List<OnnxTensor> input) {

        String outputName = "T" + ctx.count();

     //   List<OnnxTensor> output = createOutput(List.of(outputName), input, this::getOutputShapes);
        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
         //   .output(output)
          //  .valueInfos(createValueInfoProto(output))
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
                addOutputToOnnxBlockAsInDJL(onnxBlock,input, outputName);
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
                addOutputToOnnxBlockAsInDJL(onnxBlock,input, outputName);
                break;
//            case DEFLATE:
//                String shapeName2 = "Shape" + ctx.count();
//                nodeBuilder.setOpType("Reshape")
//                    .addInput(shapeName2);
//              long[] inputShape =  input.get(0).getShape().getShape();
//              long[] targetShape = new long[]
//                onnxBlock.getParameters().add(TensorProto.newBuilder()
//                    .setName(shapeName2)
//                    .setDataType(TensorProto.INT64_DATA_FIELD_NUMBER)
//                    .addAllDims(List.of(2L))
//                    .addAllInt64Data(List.of(1L, size))
//                    .build());
//                addOutputToOnnxBlockAsInDJL(onnxBlock,input, outputName);
//                break;
            case RELU:
                nodeBuilder.setOpType("Relu");
                addOutputToOnnxBlockAsInDJL(onnxBlock,input, outputName);
                break;
            case SIGMOID:
                nodeBuilder.setOpType("Sigmoid");
                addOutputToOnnxBlockAsInDJL(onnxBlock,input, outputName);
                break;
            case GLOBAL_AVG_POOL_2d:
                nodeBuilder.setOpType("GlobalAveragePool");
                addOutputToOnnxBlockAddingMissingDimensions(onnxBlock,input, outputName);
                break;
            case GLOBAL_MAX_POOL_2d:
                nodeBuilder.setOpType("GlobalMaxPool");
                addOutputToOnnxBlockAddingMissingDimensions(onnxBlock,input, outputName);
                break;
            case IDENTITY:
                nodeBuilder.setOpType("Identity");
                addOutputToOnnxBlockAsInDJL(onnxBlock,input, outputName);
                break;
            case NOT_IMPLEMENTED_YET:
            default:
                throw new NotImplementedException(type.name());
        }
        onnxBlock.getNodes().add(nodeBuilder.build());

        return onnxBlock;
    }

    public enum Type {IDENTITY, RELU, SIGMOID, MAX_POOLING, BATCH_FLATTEN, DEFLATE, GLOBAL_AVG_POOL_2d,GLOBAL_MAX_POOL_2d,  NOT_IMPLEMENTED_YET}
}
