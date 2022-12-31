package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.norm.LayerNorm;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;
import static ai.enpasos.mnist.blocks.OnnxHelper.convert;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

@SuppressWarnings("all")
public class LayerNormExt extends LayerNorm implements OnnxIO {

    LayerNormExt(LayerNormExt.Builder builder) {
        super(builder);
    }


    public static Builder builder() {
        return new Builder();
    }

    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        Shape inputShape = input.get(0).getShape();

        OnnxBlock blockLayerNormalization = nodeLayerNormalization(counter, input);

        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .valueInfos(createValueInfoProto(input))
            .output(blockLayerNormalization.getOutput())
            .build();

        onnxBlock.addChild(blockLayerNormalization);


        return onnxBlock;

    }


    private OnnxBlock nodeLayerNormalization(OnnxCounter counter, List<OnnxTensor> input) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(input.get(0).getShape())
        );

        String scale = "T" + counter.count();
        String bias = "T" + counter.count();

        String betaName = "beta" + counter.count();
        NDArray beta = this.parameters.get("beta").getArray();
        List<Long> allDimBeta = new ArrayList<>();
        allDimBeta.add(1L);
        allDimBeta.addAll(convert(beta.getShape().getShape()));

        String gammaName = "gamma" + counter.count();
        NDArray gamma = this.parameters.get("gamma").getArray();
        List<Long> allDimsGamma = new ArrayList<>();
        allDimsGamma.add(1L);
        allDimsGamma.addAll(convert(gamma.getShape().getShape()));


        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("Node" + counter.count())
                    .setOpType("LayerNormalization")
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INT)
                        .setName("axis")
                        .setI(-3)
                        .build())
                    .addInput(input.get(0).getName())
                    .addInput(gammaName)
                    .addInput(betaName)
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .parameters(List.of(
                TensorProto.newBuilder()
                    .setName(betaName)
                    .setDataType(1)
                    .addAllDims(allDimBeta)
                    .addAllFloatData(convert(beta))
                    .build(),
                TensorProto.newBuilder()
                    .setName(gammaName)
                    .setDataType(1)
                    .addAllDims(allDimsGamma)
                    .addAllFloatData(convert(gamma))
                    .build()
            ))
            .build();
    }

    public static final class Builder extends LayerNorm.Builder {

        protected Builder() {
            super();
        }

        public LayerNormExt build() {
            return new LayerNormExt(this);
        }
    }
}
