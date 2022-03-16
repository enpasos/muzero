package ai.enpasos.mnist.blocks;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;
import ai.enpasos.onnx.ValueInfoProto;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@Builder
public class OnnxBlock {
    @Builder.Default
    List<OnnxTensor> input = new ArrayList<>();

    @Builder.Default
    List<OnnxTensor> output = new ArrayList<>();

    @Builder.Default
    List<ValueInfoProto> valueInfos = new ArrayList<>();
    @Builder.Default
    List<NodeProto> nodes = new ArrayList<>();
    @Builder.Default
    List<TensorProto> parameters = new ArrayList<>();


    public static List<OnnxTensor> createOutput(List<String> outputNames, List<OnnxTensor> input, Function<Shape[], Shape[]> shapeConverter) {
        List<Shape> outputShapes = List.of(shapeConverter.apply(getShapes(input).toArray(new Shape[0])));
        return combine(outputNames, outputShapes);
    }

    public static List<Shape> getShapes(List<OnnxTensor> tensors) {
        return tensors.stream().map(OnnxTensor::getShape).collect(Collectors.toList());
    }

    public static List<String> getNames(List<OnnxTensor> tensors) {
        return tensors.stream().map(OnnxTensor::getName).collect(Collectors.toList());
    }

    public static List<OnnxTensor> combine(List<String> outputNames, List<Shape> outputShapes) {
        return IntStream.range(0, outputNames.size()).mapToObj(i ->
                OnnxTensor.builder()
                    .name(outputNames.get(i))
                    .shape(outputShapes.get(i))
                    .build())
            .collect(Collectors.toList());
    }

    public void addChild(OnnxBlock onnxBlockChild) {
        this.getParameters().addAll(onnxBlockChild.getParameters());
        this.getNodes().addAll(onnxBlockChild.getNodes());
        this.getValueInfos().addAll(onnxBlockChild.getValueInfos());
    }

    public List<Shape> getInputShapes() {
        return getShapes(input);
    }

    public List<Shape> getOutputShapes() {
        return getShapes(output);
    }

    public List<String> getInputNames() {
        return getNames(input);
    }

    public List<String> getOutputNames() {
        return getNames(output);
    }


}
