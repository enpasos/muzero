package ai.enpasos.mnist.blocks;

import ai.djl.Model;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.onnx.GraphProto;
import ai.enpasos.onnx.ModelProto;
import ai.enpasos.onnx.OperatorSetIdProto;

import java.io.FileOutputStream;
import java.util.List;
import java.util.stream.Collectors;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;

public class OnnxIOExport {


    public static void onnxExport(Model model, List<Shape> inputShapes, String fileName) {
        ModelProto modelProto = getModelProto(model, inputShapes);
        save(modelProto, fileName);
    }


    private static ModelProto getModelProto(Model model, List<Shape> inputShapes) {

        OnnxIO onnxIO = (OnnxIO) model.getBlock();


        OnnxBlock onnxBlock = onnxIO.getOnnxBlock(
            OnnxCounter.builder().counter(0).build(),
            combine(List.of("Input"), inputShapes)
        );

        ModelProto.Builder modelBuilder = ModelProto.newBuilder();


        modelBuilder.setDomain("ai.enpasos");
        modelBuilder.setProducerName("enpasos");
        modelBuilder.setIrVersion(8);
        modelBuilder.addOpsetImport(OperatorSetIdProto.newBuilder().setDomain("ai.onnx").setVersion(15).build());

        GraphProto.Builder graphBuilder = GraphProto.newBuilder();

        graphBuilder.addAllNode(onnxBlock.getNodes());

        graphBuilder.addAllInput(onnxBlock.getValueInfos().stream()
                .filter(vi -> onnxBlock.getInputNames().contains(vi.getName()))
                .collect(Collectors.toList())
        );
        graphBuilder.addAllValueInfo(onnxBlock.getValueInfos().stream()
                .filter(vi -> !onnxBlock.getOutputNames().contains(vi.getName())
                           && !onnxBlock.getInputNames().contains(vi.getName()))
                .collect(Collectors.toList())
        );
        graphBuilder.addAllOutput(onnxBlock.getValueInfos().stream()
                .filter(vi -> onnxBlock.getOutputNames().contains(vi.getName()))
                .collect(Collectors.toList())
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
