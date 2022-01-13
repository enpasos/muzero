package ai.enpasos.mnist.inference;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class TicTacToeMicrosoftRT {
    public static void main(String[] args) throws  Exception {

        String modelPath = "./models/initialInference.onnx";

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


                float[][][][] input = new float[1][3][3][3];
//
//                SparseData data = load(args[1]);
//
//                float[][][][] testData = new float[1][1][28][28];
//                float[][] testDataSKL = new float[1][780];
//
//                int correctCount = 0;
//                int[][] confusionMatrix = new int[10][10];
//
//                String inputName = session.getInputNames().iterator().next();
//
//                for (int i = 0; i < data.labels.length; i++) {
//                    if (args.length == 3) {
//                        writeDataSKL(testDataSKL, data.indices.get(i), data.values.get(i));
//                    } else {
//                        writeData(testData, data.indices.get(i), data.values.get(i));
//                    }
//
                    try (OnnxTensor test = OnnxTensor.createTensor(env, input);
                         OrtSession.Result output = session.run(Collections.singletonMap("Input0", test))) {
                        float[][] v = (float[][]) output.get("T673").get().getValue();
                        log.info("value: " + v[0][0]);
                        int i = 42;

//                        int predLabel;
//
//                        if (args.length == 3) {
//                            long[] labels = (long[]) output.get(0).getValue();
//                            predLabel = (int) labels[0];
//                        } else {
//                            float[][] outputProbs = (float[][]) output.get(0).getValue();
//                            predLabel = pred(outputProbs[0]);
//                        }
//                        if (predLabel == data.labels[i]) {
//                            correctCount++;
//                        }
//
//                        confusionMatrix[data.labels[i]][predLabel]++;
//
//                        if (i % 2000 == 0) {
//                            logger.log(Level.INFO, "Cur accuracy = " + ((float) correctCount) / (i + 1));
//                            logger.log(Level.INFO, "Output type = " + output.get(0).toString());
//                            if (args.length == 3) {
//                                logger.log(Level.INFO, "Output type = " + output.get(1).toString());
//                                logger.log(Level.INFO, "Output value = " + output.get(1).getValue().toString());
//                            }
//                        }
//                    }
                }
//
//                logger.info("Final accuracy = " + ((float) correctCount) / data.labels.length);
//
//                StringBuilder sb = new StringBuilder();
//                sb.append("Label");
//                for (int i = 0; i < confusionMatrix.length; i++) {
//                    sb.append(String.format("%1$5s", "" + i));
//                }
//                sb.append("\n");
//
//                for (int i = 0; i < confusionMatrix.length; i++) {
//                    sb.append(String.format("%1$5s", "" + i));
//                    for (int j = 0; j < confusionMatrix[i].length; j++) {
//                        sb.append(String.format("%1$5s", "" + confusionMatrix[i][j]));
//                    }
//                    sb.append("\n");
//                }
//
//                System.out.println(sb.toString());
           }
        }
//
//        logger.info("Done!");
    }
}
