package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.norm.LayerNormOpened;
import ai.enpasos.mnist.blocks.OnnxBlockExt;
import ai.enpasos.mnist.blocks.OnnxContext;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;

import java.util.List;

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
    public OnnxBlockExt getOnnxBlockExt(OnnxContext ctx) {
        OnnxBlockExt onnxBlockExt = new OnnxBlockExt();

        Shape[] inputShapes = ctx.getInputShapes().toArray(new Shape[0]);
        String gammaName = "gamma" + ctx.counter();
        String betaName = "beta" + ctx.counter();
        String normalizationOutput = "LayerNormalizationOutput" + ctx.counter();
        String mulOutput = "MulOutput" + ctx.counter();
        String addOutput = "AddOutput" + ctx.counter();

        onnxBlockExt.getNodes().add(
                NodeProto.newBuilder()
                        .setName("Node" + ctx.counter())
                        .setOpType("MeanVarianceNormalization")
                        .addAttribute(AttributeProto.newBuilder()
                                .setType(AttributeProto.AttributeType.INTS)
                                .setName("axes")
                                .addAllInts(List.of(1L, 2L, 3L))
                                .build())
                        .addInput(ctx.getInputNames().get(0))
                        .addOutput(normalizationOutput)
                        .build());


        onnxBlockExt.getValueInfos().add(createValueInfoProto(normalizationOutput, inputShapes[0]));

        onnxBlockExt.getNodes().add(

                NodeProto.newBuilder()
                        .setName("MulNode" + ctx.counter())
                        .setOpType("Mul")
                        .addInput(normalizationOutput)
                        .addInput(gammaName)
                        .addOutput(mulOutput)
                        .build());

        onnxBlockExt.getValueInfos().add(createValueInfoProto(mulOutput, inputShapes[0]));

        onnxBlockExt.getNodes().add(
                NodeProto.newBuilder()
                        .setName("AddNode" + ctx.counter())
                        .setOpType("Add")
                        .addInput(mulOutput)
                        .addInput(betaName)
                        .addOutput(addOutput)
                        .build());
        onnxBlockExt.getValueInfos().add(createValueInfoProto(addOutput, inputShapes[0]));


        onnxBlockExt.setOutputShapes(List.of(this.getOutputShapes(inputShapes)));
        onnxBlockExt.getOutputNames().add(addOutput);

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


    public static final class Builder extends LayerNormOpened.Builder {

        protected Builder() {
            super();
        }

        public LayerNormExt build() {
            return new LayerNormExt(this);
        }
    }
}
