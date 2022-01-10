package ai.enpasos.mnist.blocks.ext;

import ai.enpasos.mnist.blocks.OnnxBlockExt;
import ai.enpasos.mnist.blocks.OnnxContext;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.onnx.NodeProto;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class ReluBlockExt implements OnnxIO {
    @Override
    public OnnxBlockExt getOnnxBlockExt(OnnxContext ctx) {
        OnnxBlockExt onnxBlockExt = new OnnxBlockExt();
        String outputName = "reluOutput" + ctx.counter();
        onnxBlockExt.getNodes().add(NodeProto.newBuilder()
                .setName("reluNode" + ctx.counter())
                .setOpType("Relu")
                .addInput(ctx.getInputNames().get(0))
                .addOutput(outputName)
                .build());

        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputName, ctx.getInputShapes().get(0)));
        onnxBlockExt.getOutputNames().add(outputName);
        onnxBlockExt.setOutputShapes(ctx.getInputShapes());


        return onnxBlockExt;
    }
}
