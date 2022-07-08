package ai.enpasos.mnist.inference;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.onnxruntime.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.FloatBuffer;
import java.util.*;

@Slf4j
public class TicTacToeMicrosoftRT {
    public static void main(String[] args) throws Exception {

        List<JvmData> resultA = runFunction("./models/representation.onnx",
            List.of(
                JvmData.builder()
                    .name("InputH_0")
                    .shape(new Shape(1, 3, 3, 3))
                    .data(new float[27])
                    .build()
            ));

        resultA.get(0).setName("InputF_0");
        List<JvmData> result2 = runFunction("./models/prediction.onnx", resultA);
        log.info(Arrays.toString(softmax(result2.get(0).getData())));
        log.info(Arrays.toString(result2.get(1).getData()));
    }

    public static float[] softmax(float[] input) {
        double[] tmp = new double[input.length];
        double sum = 0.0;
        for (int i = 0; i < input.length; i++) {
            double val = Math.exp(input[i]);
            sum += val;
            tmp[i] = val;
        }

        float[] output = new float[input.length];
        for (int i = 0; i < output.length; i++) {
            if (sum == 0) throw new MuZeroException("sum should not be zero");
            output[i] = (float) (tmp[i] / sum);
        }
        return output;
    }

    private static List<JvmData> runFunction(String modelPath, List<JvmData> input) throws OrtException {

        try (OrtEnvironment env = OrtEnvironment.getEnvironment();
             OrtSession.SessionOptions opts = new OrtSession.SessionOptions()) {
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
            try (OrtSession session = env.createSession(modelPath, opts)) {
                Map<String, OnnxTensor> map = new TreeMap<>();
                try {
                    for (JvmData jvmData : input) {
                        map.put(jvmData.getName(), OnnxTensor.createTensor(env, FloatBuffer.wrap(jvmData.getData()), jvmData.getShape().getShape()));
                    }
                    OrtSession.Result output = session.run(map);
                    return convert(output);
                } finally {
                    map.values().forEach(OnnxTensor::close);
                }
            }
        }
    }


    public static List<JvmData> convert(OrtSession.Result raw) {

        List<JvmData> data = new ArrayList<>();
        for (Map.Entry<String, OnnxValue> entry : raw) {
            String name = entry.getKey();
            OnnxValue v = entry.getValue();

            data.add(JvmData.builder()
                .data(((OnnxTensor) v).getFloatBuffer().array())
                .name(name)
                .shape(new Shape(((TensorInfo) v.getInfo()).getShape()))
                .build());
        }
        return data;
    }
}
