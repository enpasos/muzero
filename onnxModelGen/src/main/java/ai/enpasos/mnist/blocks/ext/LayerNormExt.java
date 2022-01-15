package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.norm.LayerNormOpened;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;

import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;
import static ai.enpasos.mnist.blocks.OnnxHelper.convert;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

public class LayerNormExt extends LayerNormOpened implements OnnxIO {

/*
 * Workaround at least to make inference work for onnx
 * see https://github.com/onnx/onnx/issues/2379
 */
    LayerNormExt(LayerNormExt.Builder builder) {
        super(builder);
    }


    public static Builder builder() {
        return new Builder();
    }

    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        Shape inputShape = input.get(0).getShape();
        Shape condensedShape = new Shape(inputShape.get(0),inputShape.get(1) * inputShape.get(2) * inputShape.get(3));
        Shape fullyCondensedShape = new Shape(inputShape.get(0) * inputShape.get(1) * inputShape.get(2) * inputShape.get(3));


        OnnxBlock blockReshape = nodeReshape(counter, input, condensedShape);
        OnnxBlock blockTranspose = nodeTranspose(counter,  blockReshape.getOutput());

        String betaName = "beta" + counter.count();
        NDArray beta = this.parameters.get("beta").getArray();   // transpose??
        String gammaName = "gamma" + counter.count();
        NDArray gamma = this.parameters.get("gamma").getArray();  // transpose??

        OnnxBlock blockDataGamma = nodeData(counter, gamma, fullyCondensedShape);
        OnnxBlock blockDataBeta = nodeData(counter, beta, fullyCondensedShape);



        OnnxBlock blockBatchNormalization = nodeBatchNormalization(counter, blockTranspose.getOutput(), blockDataGamma.getOutput(), blockDataBeta.getOutput(), beta);
        OnnxBlock blockTranspose2 = nodeTranspose(counter,  blockBatchNormalization.getOutput());
        OnnxBlock blockReshape2 = nodeReshape(counter, blockTranspose2.getOutput(), inputShape);



        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .valueInfos(createValueInfoProto(input))
            .output(blockReshape2.getOutput())
            .build();

        onnxBlock.addChild(blockReshape);
        onnxBlock.addChild(blockTranspose);
        onnxBlock.addChild(blockDataGamma);
        onnxBlock.addChild(blockDataBeta);
        onnxBlock.addChild(blockBatchNormalization);
        onnxBlock.addChild(blockTranspose2);
        onnxBlock.addChild(blockReshape2);

        return onnxBlock;

    }


  private OnnxBlock nodeReshape(OnnxCounter counter, List<OnnxTensor> input, Shape targetShape) {
      List<OnnxTensor> output = combine(
          List.of("T" + counter.count()),
          List.of(targetShape)
      );
      String shapeName = "T" + counter.count();
      return OnnxBlock.builder()
          .input(input)
          .output(output)
          .valueInfos(createValueInfoProto(output))
          .nodes(List.of(
              NodeProto.newBuilder()
                  .setName("Node" + counter.count())
                  .setOpType("Reshape")
                  .addInput(input.get(0).getName())
                  .addInput(shapeName)
                  .addOutput(output.get(0).getName())
                  .build()
          ))
          .parameters(List.of(
              TensorProto.newBuilder()
                  .setName(shapeName)
                  .setDataType(TensorProto.INT64_DATA_FIELD_NUMBER)
                  .addAllDims(List.of(2L))
                  .addAllInt64Data(convert(targetShape.getShape()))
                  .build()
          ))
          .build();
  }

    private OnnxBlock nodeTranspose(OnnxCounter counter, List<OnnxTensor> input) {
        Shape inputShape = input.get(0).getShape();
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(new Shape(inputShape.get(1), inputShape.get(0)))
        );
        String shapeName = "T" + counter.count();
        return OnnxBlock.builder()
            .input(input)
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("Node" + counter.count())
                    .setOpType("Transpose")
//                    .addAttribute(AttributeProto.newBuilder()
//                        .setType(AttributeProto.AttributeType.INTS)
//                        .setName("perm")
//                        .addAllInts(List.of(1L, 0L))
//                        .build())
                    .addInput(input.get(0).getName())
                    .addOutput(output.get(0).getName())
                    .build()
            ))

            .build();
    }

    private OnnxBlock nodeBatchNormalization(OnnxCounter counter, List<OnnxTensor> input,List<OnnxTensor> gamma,List<OnnxTensor> beta, NDArray betaData) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(input.get(0).getShape())
        );

        String dummyRunningMean = "T" + counter.count();
        String dummyRunningVar = "T" + counter.count();
        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("Node" + counter.count())
                    .setOpType("BatchNormalization")
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.FLOAT)
                        .setName("momentum")   // do not use running_mean
                        .setF(0f)
                        .build())
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INT)
                        .setName("training_mode")
                        .setI(0)   // default
                        .build())
                    .addInput(input.get(0).getName())
                    .addInput(gamma.get(0).getName())
                    .addInput(beta.get(0).getName())
                    .addInput(dummyRunningMean)
                    .addInput(dummyRunningVar)
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .parameters(List.of(
                TensorProto.newBuilder()
                    .setName(dummyRunningMean)
                    .setDataType(1)
                    .addAllDims(convert(beta.get(0).getShape().getShape()))
                    .addAllFloatData(  convert(betaData.toFloatArray() ))
                    .build(),
                TensorProto.newBuilder()
                    .setName(dummyRunningVar)
                    .setDataType(1)
                    .addAllDims(convert(beta.get(0).getShape().getShape()))
                    .addAllFloatData(  convert(betaData.toFloatArray() ))
                    .build()
            ))
            .build();
    }


    private OnnxBlock nodeData(OnnxCounter ctx, NDArray data, Shape outputShape) { //}, onnxBlock onnxBlock, String outputName, Shape shape) {
        List<OnnxTensor> output =  combine(List.of("T" + ctx.count()), List.of(outputShape));

      //  NDArray weight = this.parameters.get("weight").getArray().transpose();

        String parameterName1 = "P"+ ctx.count();
        String parameterName2 = "P"+ ctx.count();

        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("Node" + ctx.count())
                    .setOpType("Reshape")
                    .addInput(parameterName1)
                    .addInput(parameterName2)
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .parameters(List.of(
                // data
                TensorProto.newBuilder()
                    .setName(parameterName1)
                    .setDataType(1)
                    .addAllDims(convert(data.getShape().getShape()))
                    .addAllFloatData(convert(data))
                    .build(),
                // shape
                TensorProto.newBuilder()
                    .setName(parameterName2)
                    .setDataType(TensorProto.INT64_DATA_FIELD_NUMBER)
                    .addAllDims(List.of(1L))
                    .addAllInt64Data(convert(outputShape.getShape()))
                    .build()
            ))
            .valueInfos(
                createValueInfoProto(output)
            )
            .build();

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
