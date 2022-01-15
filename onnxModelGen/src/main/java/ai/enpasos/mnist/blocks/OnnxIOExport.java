package ai.enpasos.mnist.blocks;

import ai.djl.Model;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.onnx.GraphProto;
import ai.enpasos.onnx.ModelProto;
import ai.enpasos.onnx.OperatorSetIdProto;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;

public class OnnxIOExport {


    public static void onnxExport(Model model, List<Shape> inputShapes, String fileName, String namePrefix) {
        onnxExport((OnnxIO) model.getBlock(),  inputShapes,  fileName, namePrefix);
    }

    public static void onnxExport(OnnxIO onnxIO, List<Shape> inputShapes, String fileName, String namePrefix) {
        ModelProto modelProto = getModelProto(onnxIO, inputShapes, namePrefix);
        save(modelProto, fileName);
    }

    private static ModelProto getModelProto(OnnxIO onnxIO, List<Shape> inputShapes, String namePrefix) {
        OnnxCounter counter = OnnxCounter.builder().counter(0).prefix(namePrefix).build();

        List<String> inputNames = new ArrayList<>();
        IntStream.range(0, inputShapes.size()).forEach(i -> inputNames.add("Input"+ counter.count()));

        OnnxBlock onnxBlock = onnxIO.getOnnxBlock(
            counter,
            combine(inputNames, inputShapes)
        );

        ModelProto.Builder modelBuilder = ModelProto.newBuilder();


        modelBuilder.setDomain("ai.enpasos");
        modelBuilder.setProducerName("enpasos");
        modelBuilder.setIrVersion(8);
        modelBuilder.addOpsetImport(OperatorSetIdProto.newBuilder().setDomain("ai.onnx").setVersion(15).build());

        GraphProto.Builder graphBuilder = GraphProto.newBuilder();

        graphBuilder.addAllNode(onnxBlock.getNodes());


        onnxBlock.getInputNames().stream().forEach(
            n ->  graphBuilder.addInput(
                        onnxBlock.getValueInfos().stream()
                            .filter(vi ->  vi.getName().equals(n))
                            .findFirst().get()
            )
        );

        graphBuilder.addAllValueInfo(onnxBlock.getValueInfos().stream()
                .filter(vi -> !onnxBlock.getOutputNames().contains(vi.getName())
                           && !onnxBlock.getInputNames().contains(vi.getName()))
                .collect(Collectors.toSet())
        );

        onnxBlock.getOutputNames().stream().forEach(
            n ->  graphBuilder.addOutput(
                onnxBlock.getValueInfos().stream()
                    .filter(vi ->  vi.getName().equals(n))
                    .findFirst().get()
            )
        );

        graphBuilder.addAllInitializer(onnxBlock.getParameters());


        modelBuilder.setGraph(graphBuilder);
        return modelBuilder.build();
    }


    private static void save(ModelProto modelProto, String fileName) {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            byte[] data = modelProto.toByteArray();
            fos.write(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
