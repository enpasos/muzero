package ai.enpasos.mnist;

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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.FloatBuffer;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ai.enpasos.mnist.blocks.OnnxIOExport.onnxExport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class MnistBlockTest {

    @Test
    void newMnistBlock() throws Exception {

        compareOnnxWithDJL(
            "./target/MnistBlock.onnx",
            MnistBlock.newMnistBlock(),
            List.of(new Shape(1, 1, 28, 28)));

    }

    private void compareOnnxWithDJL(String modelPath, Block block, List<Shape> inputShapes) throws OrtException {
        Pair<NDList, NDList> inputOutput =   inputOutputFromDJL(block, inputShapes, modelPath);
        NDList input = inputOutput.getKey();
        NDList outputDJL = inputOutput.getValue();

        NDList outputOnnx = outputFromOnnx(modelPath, input);


        assertEquals(outputDJL.size(), outputOnnx.size());
        for (int i = 0; i < outputDJL.size(); i++) {
            boolean check = outputDJL.get(i).allClose(outputOnnx.get(i), 1e-04, 1e-04, true);
            if (!check) {
                log.error("DJL ... " + Arrays.toString(outputDJL.get(i).toFloatArray()));
                log.error("Onnx ... " + Arrays.toString(outputOnnx.get(i).toFloatArray()));
            }
            assertTrue(check);
        }
    }


    public static Pair<NDList, NDList> inputOutputFromDJL(Block block,List<Shape> inputShapes, String modelPath) {


        NDList input_ = null;
        NDList outputDJL = null;
        try (Model model = Model.newInstance("mymodel", Device.cpu())) {

            model.setBlock(block);



            input_ = new NDList(inputShapes.stream()
                .map(inputShape -> NDManager.newBaseManager().randomUniform(0f, 1f, inputShape))
                .collect(Collectors.toList()));

            // no training here - loss function is a dummy
            try (Trainer trainer = model.newTrainer(new DefaultTrainingConfig(Loss.l2Loss()))) {
                outputDJL = trainer.forward(input_).toDevice(Device.cpu(), true);
            } catch (Exception e) {
            }

            onnxExport(model, inputShapes, modelPath);

        } catch (Exception e) {
            e.printStackTrace();
            String message = "not able to save created model";
            log.error(message);
        }
        return new Pair<>(input_, outputDJL);
    }

    private NDList outputFromOnnx(String modelPath, NDList input) throws OrtException {
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

                Shape outputShape = new Shape(1, 10);
                // output [1, 10]

                try (
                    // TODO generalize
                    OnnxTensor inputOnnx = OnnxTensor.createTensor(env, FloatBuffer.wrap(input.get(0).toFloatArray()), input.get(0).getShape().getShape());
                    OrtSession.Result output = session.run(Collections.singletonMap("Input0", inputOnnx));) {

                    OnnxTensor t = (OnnxTensor) output.get(0);
                    outputOnnx = new NDList(NDManager.newBaseManager().create(t.getFloatBuffer().array(), outputShape));

                }

            }
        }
        return outputOnnx;
    }


}
