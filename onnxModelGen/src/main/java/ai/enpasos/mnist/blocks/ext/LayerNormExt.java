package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.norm.LayerNormOpened;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;

import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;
import static ai.enpasos.mnist.blocks.OnnxHelper.convert;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class LayerNormExt extends LayerNormOpened implements OnnxIO {


    LayerNormExt(LayerNormExt.Builder builder) {
        super(builder);
    }

    /**
     * Creates a builder to build a {@code LayerNorm}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {

        OnnxBlock blockMVN = nodeMVN(counter, input);
        OnnxBlock blockMul = nodeMul(counter, blockMVN.getOutput());
        OnnxBlock blockAdd = nodeAdd(counter, blockMul.getOutput());

        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .output(blockAdd.getOutput())
            .valueInfos(createValueInfoProto(blockAdd.getOutput()))
            .build();

        onnxBlock.addChild(blockMVN);
        onnxBlock.addChild(blockMul);
        onnxBlock.addChild(blockAdd);

        return onnxBlock;

    }

    private OnnxBlock nodeMVN(OnnxCounter counter, List<OnnxTensor> input) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(input.get(0).getShape())
        );
        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("Node" + counter.count())
                    .setOpType("MeanVarianceNormalization")
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("axes")
                        .addAllInts(List.of(1L, 2L, 3L))
                        .build())
                    .addInput(input.get(0).getName())
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .valueInfos(createValueInfoProto(output))
            .build();
    }

    private OnnxBlock nodeMul(OnnxCounter counter, List<OnnxTensor> input) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(input.get(0).getShape())
        );
        String gammaName = "gamma" + counter.count();
        NDArray gamma = this.parameters.get("gamma").getArray();
        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("Mul")
                    .addInput(input.get(0).getName())
                    .addInput(gammaName)
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .parameters(List.of(
                TensorProto.newBuilder()
                    .setName(gammaName)
                    .setDataType(1)
                    .addAllDims(convert(gamma.getShape().getShape()))
                    .addAllFloatData(convert(gamma))
                    .build()
            ))
            .valueInfos(createValueInfoProto(output))
            .build();
    }
    private OnnxBlock nodeAdd(OnnxCounter counter, List<OnnxTensor> input) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(input.get(0).getShape())
        );
        String betaName = "beta" + counter.count();
        NDArray beta = this.parameters.get("beta").getArray();
        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("Add")
                    .addInput(input.get(0).getName())
                    .addInput(betaName)
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .parameters(List.of(
                TensorProto.newBuilder()
                    .setName(betaName)
                    .setDataType(1)
                    .addAllDims(convert(beta.getShape().getShape()))
                    .addAllFloatData(convert(beta))
                    .build()
            ))
            .valueInfos(createValueInfoProto(output))
            .build();

    }


    public static final class Builder extends LayerNormOpened.Builder {

        protected Builder() {
            super();
        }

        public LayerNormExt build() {
            return new LayerNormExt(this);
        }
    }
}
