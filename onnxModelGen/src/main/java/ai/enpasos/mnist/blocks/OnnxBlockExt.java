package ai.enpasos.mnist.blocks;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;
import ai.enpasos.onnx.ValueInfoProto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OnnxBlockExt  {
    List<String> inputNames;
    List<String> outputNames;
    List<Shape> outputShapes;
    List<ValueInfoProto> valueInfos;
    List<NodeProto> nodes;
    List<TensorProto> parameters;


    public OnnxBlockExt( ) {
        this.nodes = new ArrayList<>();
        this.parameters = new ArrayList<>();
        this.valueInfos = new ArrayList<>();

        inputNames = new ArrayList<>();
        outputNames = new ArrayList<>();
        outputShapes = new ArrayList<>();
    }

    public void addChild(OnnxContext ctx, OnnxBlockExt onnxBlockExtChild) {
        this.getParameters().addAll(onnxBlockExtChild.getParameters());
        this.getNodes().addAll(onnxBlockExtChild.getNodes());
        this.getValueInfos().addAll(onnxBlockExtChild.getValueInfos());
    }
}
