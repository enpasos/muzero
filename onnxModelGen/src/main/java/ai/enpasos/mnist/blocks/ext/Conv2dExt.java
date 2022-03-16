package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.convolutional.Conv2dOpened;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;
import com.google.protobuf.ByteString;

import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.createOutput;
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
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {

        List<OnnxTensor> output = createOutput(List.of("T" + counter.count()), input, this::getOutputShapes);
        NDArray weights = this.getParameters().get("weight").getArray();
        String parameterName = "P" + counter.count();

        return OnnxBlock.builder()
            .input(input)
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .parameters(List.of(
                TensorProto.newBuilder()
                    .setName(parameterName)
                    .setDataType(1)
                    .addAllDims(convert(weights.getShape().getShape()))
                    .addAllFloatData(convert(weights))
                    .build()
            ))
            .nodes(List.of(
                    NodeProto.newBuilder()
                        .setName("N" + counter.count())
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
                        .addInput(input.get(0).getName())
                        .addInput(parameterName)
                        .addOutput(output.get(0).getName())
                        .build()
                )
            ).build();
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
        @Override
        public Conv2dExt build() {
            validate();
            return new Conv2dExt(this);
        }
    }
}
