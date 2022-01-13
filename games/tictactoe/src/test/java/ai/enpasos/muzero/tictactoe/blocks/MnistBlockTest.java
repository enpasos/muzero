package ai.enpasos.muzero.tictactoe.blocks;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.onnxruntime.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
class MnistBlockTest {

    public static void main(String[] args) throws Exception {

        String modelPath = "./models/mnist.onnx";

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

                // input [1, 1, 28, 28]
                Shape inputShape = new Shape(1, 1, 28, 28);
                NDArray inputArray = NDManager.newBaseManager().randomUniform(0f, 1f, inputShape);

                // output [1, 10]

                try (
                    OnnxTensor inputOnnx = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputArray.toFloatArray()), inputShape.getShape());
                    OrtSession.Result output = session.run(Collections.singletonMap("Input", inputOnnx));) {

                    OnnxValue value = output.get("T109").get();
                    float[][] v = (float[][]) value.getValue();
                    log.info("value: " + Arrays.toString(v[0]));
                }

            }
        }



    }

    @Test
    void newMnistBlock() {
    }
}
