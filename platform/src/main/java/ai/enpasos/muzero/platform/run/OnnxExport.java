package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.agent.d_model.Network;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.InitialInferenceBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.RecurrentInferenceBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.SimilarityPredictorBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.SimilarityProjectorBlock;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static ai.enpasos.mnist.blocks.OnnxIOExport.onnxExport;
import static java.util.Map.entry;

@Slf4j
@Component
public class OnnxExport {

    public static final String ONNX = "onnx/";
    @Autowired
    MuZeroConfig config;

    @Autowired
    BatchFactory networkHelfer;


    private OnnxExport() {
    }

    @SuppressWarnings("squid:S106")
    public void run(List<Shape> inputRepresentation, List<Shape> inputPrediction, List<Shape> inputGeneration, List<Shape> inputSimilarityPredictor, List<Shape> inputSimilarityProjector, int epoch) {

        try {
            FileUtils.forceMkdir(new File(config.getOutputDir() + "onnx"));
            try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
                Network network = null;
                if (epoch == -1) {
                    network = new Network(config, model);
                } else {
                    network = new Network(config, model, Paths.get(config.getNetworkBaseDir()), Map.ofEntries(entry("epoch", epoch + "")));
                }
                InitialInferenceBlock initialInferenceBlock = (InitialInferenceBlock) network.getInitialInference().getBlock();
               RecurrentInferenceBlock recurrentInferenceBlock = (RecurrentInferenceBlock) network.getRecurrentInference().getBlock();
//                SimilarityPredictorBlock similarityPredictorBlock = (SimilarityPredictorBlock) network.getPredictor().getBlock();
//                SimilarityProjectorBlock similarityProjectorBlock = (SimilarityProjectorBlock) network.getProjector().getBlock();
             //   RewardBlock rewardBlock = (RewardBlock) network.getReward().getBlock();


                onnxExport(initialInferenceBlock, inputRepresentation, config.getOutputDir() + ONNX + config.getModelName() + "-InitialInference.onnx", "I_");

                onnxExport(recurrentInferenceBlock, inputGeneration, config.getOutputDir() + ONNX + config.getModelName() + "-RecurrentInference.onnx", "R_");
//
//                onnxExport(initialInferenceBlock.getH(), inputRepresentation, config.getOutputDir() + ONNX + config.getModelName() + "-Representation.onnx", "H_");
//                onnxExport(initialInferenceBlock.getF(), inputPrediction, config.getOutputDir() + ONNX + config.getModelName() + "-Prediction.onnx", "F_");
//                onnxExport(recurrentInferenceBlock.getG(), inputGeneration, config.getOutputDir() + ONNX + config.getModelName() + "-Generation.onnx", "G_");
//
//                onnxExport(similarityPredictorBlock, inputSimilarityPredictor, config.getOutputDir() + ONNX + config.getModelName() + "-SimilarityPredictor.onnx", "SPRE_");
//                onnxExport(similarityProjectorBlock, inputSimilarityProjector, config.getOutputDir() + ONNX + config.getModelName() + "-SimilarityProjector.onnx", "SPRO_");
           //     onnxExport(rewardBlock, inputPrediction, config.getOutputDir() + ONNX + config.getModelName() + "-Reward.onnx", "R_");
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

    }


}
