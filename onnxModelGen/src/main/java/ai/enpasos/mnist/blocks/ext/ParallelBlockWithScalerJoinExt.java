package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.ParallelBlock;
import ai.djl.util.Pair;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.createOutput;
import static ai.enpasos.mnist.blocks.OnnxBlock.getNames;
import static ai.enpasos.mnist.blocks.OnnxHelper.convert;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class ParallelBlockWithScalerJoinExt extends ParallelBlock implements OnnxIO {


    public ParallelBlockWithScalerJoinExt(List<Block> blocks) {
        super(
            list -> {
                NDArray scaler = list.get(0).singletonOrThrow();
                Shape newShape = scaler.getShape().add(1, 1);
                scaler = scaler.reshape(newShape);
                NDArray original = list.get(1).singletonOrThrow();
                return new NDList(
                    original.mul(scaler)
                );
            },
            blocks);
    }


    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .valueInfos(createValueInfoProto(input))
            .build();

        List<OnnxTensor> outputsToBeCombined = new ArrayList<>();

        for (int i = 0; i < this.getChildren().size(); i++) {
            Pair<String, Block> p = this.getChildren().get(i);
            OnnxIO onnxIO = (OnnxIO) p.getValue();
            OnnxBlock child = onnxIO.getOnnxBlock(counter, input);
            onnxBlock.addChild(child);
            if (i == 0) { // the scaler
                OnnxTensor inputB = child.getOutput().get(0);

                OnnxTensor newOutput = OnnxTensor.builder()
                    .name("T" + counter.count())
                    .shape(inputB.getShape().add(1, 1))
                    .build();

                String shapeParam = "P" + counter.count();
                OnnxBlock deflateBlock = OnnxBlock.builder()
                    .input(List.of(inputB))
                    .output(List.of(newOutput))
                    .valueInfos(createValueInfoProto(List.of(newOutput)))
                    .nodes(List.of(
                        NodeProto.newBuilder()
                            .setName("N" + counter.count())
                            .setOpType("Reshape")
                            .addInput(inputB.getName())
                            .addInput(shapeParam)
                            .addOutput(newOutput.getName())
                            .build()
                    ))
                    .parameters(List.of(
                        // shape
                        TensorProto.newBuilder()
                            .setName(shapeParam)
                            .setDataType(TensorProto.INT64_DATA_FIELD_NUMBER)
                            .addAllDims(List.of(4L))
                            .addAllInt64Data(convert(newOutput.getShape().getShape()))
                            .build()
                    ))
                    .build();
                onnxBlock.addChild(deflateBlock);
                //   outputsToBeCombined.add(child.getOutput().get(0));
                outputsToBeCombined.add(deflateBlock.getOutput().get(0));
                //  if (child.getOutput().size() > 1) throw new RuntimeException("each output is assumed to be a single tensor here");
            } else {
                outputsToBeCombined.add(child.getOutput().get(0));
            }
        }

        // here first one of the children is the scaler


        List<OnnxTensor> output = createOutput(List.of("T" + counter.count()), input, this::getOutputShapes);


//


        String intermediate = "T" + counter.count();

        OnnxBlock concatBlock = OnnxBlock.builder()
            .input(outputsToBeCombined)
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("Mul")
                    .addAllInput(getNames(outputsToBeCombined))
                    .addOutput(output.get(0).getName())
                    .build()
//                NodeProto.newBuilder()
//                    .setName("N" + counter.count())
//                    .setOpType("Reshape")
//                    .addInput( intermediate )
//                    .addOutput(output.get(0).getName())
//                    .build()
            ))
            //    .valueInfos(createValueInfoProto(combine(List.of(intermediate), List.of())))
            .valueInfos(createValueInfoProto(output))
            .build();

        onnxBlock.addChild(concatBlock);
        onnxBlock.setOutput(concatBlock.getOutput());

        return onnxBlock;
    }
}
