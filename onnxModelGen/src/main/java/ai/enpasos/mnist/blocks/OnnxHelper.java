package ai.enpasos.mnist.blocks;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.onnx.TensorShapeProto;
import ai.enpasos.onnx.TypeProto;
import ai.enpasos.onnx.ValueInfoProto;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OnnxHelper {

    private OnnxHelper() {}

    public static TensorShapeProto convert(Shape shapes) {
        TensorShapeProto.Builder builder = TensorShapeProto.newBuilder();
        Arrays.stream(shapes.getShape()).forEach(shape ->
            builder.addDim(TensorShapeProto.Dimension.newBuilder().setDimValue(shape).build())
        );
        return builder.build();
    }

    public static List<Float> convert(NDArray ndArray) {
        float[] raw = ndArray.flatten().toFloatArray();
        return IntStream.range(0, raw.length)
            .mapToObj(i -> raw[i])
            .collect(Collectors.toList());
    }

    public static List<Long> convert(long[] array) {
        return Arrays.asList(ArrayUtils.toObject(array));
    }

    public static List<Float> convert(float[] array) {
        return Arrays.asList(ArrayUtils.toObject(array));
    }


    public static List<ValueInfoProto> createValueInfoProto(List<OnnxTensor> output) {
        return output.stream().map(OnnxHelper::createValueInfoProto).collect(Collectors.toList());

    }

    public static ValueInfoProto createValueInfoProto(OnnxTensor output) {
        ValueInfoProto valueInfoProto = ValueInfoProto.newBuilder()
            .setType(TypeProto.newBuilder()
                .setTensorType(TypeProto.Tensor.newBuilder()
                    .setElemType(1) // float32
                    .setShape(convert(output.getShape()))

                    .build())
                .build())
            .setName(output.getName())
            .build();
        return valueInfoProto;
    }
}
