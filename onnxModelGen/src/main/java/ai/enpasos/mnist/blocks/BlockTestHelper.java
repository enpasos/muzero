package ai.enpasos.mnist.blocks;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.loss.Loss;
import ai.djl.util.Pair;
import ai.enpasos.mnist.blocks.MnistBlock;
import ai.onnxruntime.*;
import ai.onnxruntime.OnnxTensor;
import lombok.extern.slf4j.Slf4j;

import java.nio.FloatBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static ai.enpasos.mnist.blocks.OnnxIOExport.onnxExport;

@Slf4j
public
class BlockTestHelper {

    public enum Testdata {RANDOM, ZERO}

    public static boolean compareOnnxWithDJL(String modelPath, Block block, List<Shape> inputShapes, Testdata testdata) throws OrtException {
        Pair<NDList, NDList> inputOutput =   inputOutputFromDJL(block, inputShapes, modelPath, testdata);
        NDList input = inputOutput.getKey();
        NDList outputDJL = inputOutput.getValue();
        NDList outputOnnx = outputFromOnnx(modelPath, input);

        boolean checkResult =  outputDJL.size() == outputOnnx.size();
        for (int i = 0; i < outputDJL.size(); i++) {
            boolean check = outputDJL.get(i).allClose(outputOnnx.get(i), 1e-04, 1e-04, false);
            if (!check) {
                log.error("DJL ... " + Arrays.toString(outputDJL.get(i).toFloatArray()));
                log.error("Onnx ... " + Arrays.toString(outputOnnx.get(i).toFloatArray()));
            }
            checkResult = checkResult && check;
        }
        return checkResult;
    }


    public static Pair<NDList, NDList> inputOutputFromDJL(Block block,List<Shape> inputShapes, String modelPath, Testdata testdata) {

        NDList input = null;
        NDList outputDJL = null;
        try (Model model = Model.newInstance("mymodel", Device.cpu())) {

            model.setBlock(block);
switch(testdata) {
    case ZERO:
        input = new NDList(inputShapes.stream()
            .map(inputShape -> NDManager.newBaseManager().create(inputShape))
            .collect(Collectors.toList()));
        break;
    case RANDOM:
        input = new NDList(inputShapes.stream()
           .map(inputShape -> NDManager.newBaseManager().randomUniform(0f, 1f, inputShape))

            .collect(Collectors.toList()));
        break;
}


            // no training here - loss function is a dummy
            try (Trainer trainer = model.newTrainer(new DefaultTrainingConfig(Loss.l2Loss()))) {
                outputDJL = trainer.forward(input).toDevice(Device.cpu(), true);
            } catch (Exception e) {
            }

            onnxExport(model, inputShapes, modelPath);

        } catch (Exception e) {
            e.printStackTrace();
            String message = "not able to save created model";
            log.error(message);
        }
        return new Pair<>(input, outputDJL);
    }

    public static NDList outputFromOnnx(String modelPath, NDList input) throws OrtException {
        NDList outputOnnx;
        try (OrtEnvironment env = OrtEnvironment.getEnvironment();
             OrtSession.SessionOptions opts = new OrtSession.SessionOptions()) {

            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

            log.info("Loading model from " + modelPath);
            try (OrtSession session = env.createSession(modelPath, opts)) {

                log.info("Inputs:");
                for (NodeInfo i : session.getInputInfo().values()) {
                    log.info(i.toString());
                }

                log.info("Outputs:");
                for (NodeInfo i : session.getOutputInfo().values()) {
                    log.info(i.toString());
                }

                List<Shape> outputShapes = session.getOutputInfo().values().stream()
                    .map(info -> new Shape(((TensorInfo)info.getInfo()).getShape()))
                    .collect(Collectors.toList());

                Map<String, ai.onnxruntime.OnnxTensor> inputMap = new TreeMap<>();
                try {

                    for (int i = 0; i < input.size(); i++) {
                        inputMap.put("Input" + i, ai.onnxruntime.OnnxTensor.createTensor(env, FloatBuffer.wrap(input.get(i).toFloatArray()), input.get(i).getShape().getShape()));
                    }
                    OrtSession.Result output = session.run(inputMap);


                    List<NDArray> ndArrays = new ArrayList<>();
                    for (int i = 0; i < output.size(); i++) {
                        ai.onnxruntime.OnnxTensor t = (OnnxTensor) output.get(i);
                        ndArrays.add(NDManager.newBaseManager().create(t.getFloatBuffer().array(), outputShapes.get(i)));
                    }

                    outputOnnx = new NDList(ndArrays);

                }
                finally {
                    inputMap.values().stream().forEach(t -> t.close());
                }

            }
        }
        return outputOnnx;
    }


}
