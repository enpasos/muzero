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
        NodeProto.Builder nodeBuilder = NodeProto.newBuilder();
        nodeBuilder.setName("reluNode" + ctx.counter());
        nodeBuilder.setOpType("Relu");
        nodeBuilder.addInput(ctx.getInputNames().get(0));
       String outputName = "reluOutput" + ctx.counter();
        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputName, ctx.getInputShapes().get(0)));
        onnxBlockExt.getOutputNames().add(outputName);
        onnxBlockExt.setOutputShapes(ctx.getInputShapes());
        nodeBuilder.addOutput(outputName);
        onnxBlockExt.getNodes().add(nodeBuilder.build());
        return onnxBlockExt;
    }
}
