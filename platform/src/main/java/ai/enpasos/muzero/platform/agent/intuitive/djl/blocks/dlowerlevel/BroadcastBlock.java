package ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.core.Linear;
import ai.djl.nn.core.LinearOpened;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.djl.util.Preconditions;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;

import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;
import static ai.enpasos.mnist.blocks.OnnxBlock.createOutput;
import static ai.enpasos.mnist.blocks.OnnxHelper.convert;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

@SuppressWarnings("all")
public class BroadcastBlock extends LinearOpened implements OnnxIO {


    BroadcastBlock(Builder builder) {
        super(builder);
    }

    /**
     * Creates a builder to build a {@code Linear}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }


    @Override
    protected NDList forwardInternal(
        ParameterStore parameterStore,
        NDList inputs,
        boolean training,
        PairList<String, Object> params) {
        NDArray current = inputs.head();

        Shape origShape = current.getShape();
        Shape shape2 = new Shape(origShape.get(0) * origShape.get(1), origShape.get(2) * origShape.get(3));
        NDArray current2 = current.reshape(shape2);
        NDArray current3 = super.forwardInternal(parameterStore, new NDList(current2), training, params).head();
        NDArray current4 = current3.reshape(origShape);

        return new NDList(current4);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        return inputs;
    }



    @Override
    // TODO
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {


        List<OnnxTensor> output = createOutput(List.of("T" + counter.count()), input, this::getOutputShapes);

        Shape outputShape = output.get(0).getShape();

        long inputDim = input.get(0).getShape().get(1);
        long outputDim = outputShape.get(1);

        OnnxBlock blockW = nodeW(counter, new Shape(new long[]{inputDim, outputDim}));
        OnnxBlock blockMult = nodeMult(counter, input.get(0), blockW.getOutput().get(0));
        OnnxBlock blockB = nodeB(counter, blockMult.getOutput());

        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .output(blockB.getOutput())
            .build();

        onnxBlock.addChild(blockW);
        onnxBlock.addChild(blockMult);
        onnxBlock.addChild(blockB);

        return onnxBlock;
    }

    private OnnxBlock nodeB(OnnxCounter counter, List<OnnxTensor> input) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(input.get(0).getShape())
        );
        String parameterName = "P" + counter.count();
        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("Add")
                    .addInput(input.get(0).getName())
                    .addInput(parameterName)
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .parameters(List.of(
                TensorProto.newBuilder()
                    .setName(parameterName)
                    .setDataType(1)
                    .addAllDims(List.of(1L, this.parameters.get("bias").getArray().size()))
                    .addAllFloatData(convert(this.parameters.get("bias").getArray()))
                    .build()
            ))
            .build();
    }

    private OnnxBlock nodeMult(OnnxCounter counter, OnnxTensor inputA, OnnxTensor inputB) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(new Shape(inputA.getShape().get(0), inputB.getShape().get(1)))
        );
        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("Mul" + counter.count())
                    .setOpType("MatMul")
                    .addInput(inputA.getName())
                    .addInput(inputB.getName())
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .build();
    }

    private OnnxBlock nodeW(OnnxCounter ctx, Shape outputShape) { //}, onnxBlock onnxBlock, String outputName, Shape shape) {
        List<OnnxTensor> output = combine(List.of("T" + ctx.count()), List.of(outputShape));

        NDArray weight = this.parameters.get("weight").getArray().transpose();

        String parameterName1 = "P" + ctx.count();
        String parameterName2 = "P" + ctx.count();

        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("Node" + ctx.count())
                    .setOpType("Reshape")
                    .addInput(parameterName1)
                    .addInput(parameterName2)
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .parameters(List.of(
                // data
                TensorProto.newBuilder()
                    .setName(parameterName1)
                    .setDataType(1)
                    .addAllDims(convert(weight.getShape().getShape()))
                    .addAllFloatData(convert(weight))
                    .build(),
                // shape
                TensorProto.newBuilder()
                    .setName(parameterName2)
                    .setDataType(TensorProto.INT64_DATA_FIELD_NUMBER)
                    .addAllDims(List.of(2L))
                    .addAllInt64Data(convert(outputShape.getShape()))
                    .build()
            ))
            .valueInfos(
                createValueInfoProto(output)
            )
            .build();

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
        public BroadcastBlock build() {
             Preconditions.checkArgument(units > 0, "You must specify unit");
            return new BroadcastBlock(this);
        }
    }
}
