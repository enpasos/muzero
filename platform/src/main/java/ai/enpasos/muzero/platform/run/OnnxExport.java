package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference.InitialInferenceBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference.RecurrentInferenceBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.cmainfunctions.SimilarityPredictorBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.cmainfunctions.SimilarityProjectorBlock;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.StringWriter;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxIOExport.onnxExport;

@Slf4j
@Component
public class OnnxExport {

    public static final String ONNX = "onnx/";
    @Autowired
    MuZeroConfig config;

    @Autowired
    NetworkHelper networkHelfer;


    private OnnxExport() {
    }

    @SuppressWarnings("squid:S106")
    public void run(List<Shape> inputRepresentation, List<Shape> inputPrediction, List<Shape> inputGeneration, List<Shape> inputSimilarityPredictor, List<Shape> inputSimilarityProjector) {

        try {
            FileUtils.forceMkdir(new File(config.getOutputDir() + "onnx"));
            try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
                Network network = new Network(config, model);
                InitialInferenceBlock initialInferenceBlock = (InitialInferenceBlock) network.getInitialInference().getBlock();
                RecurrentInferenceBlock recurrentInferenceBlock = (RecurrentInferenceBlock) network.getRecurrentInference().getBlock();
                SimilarityPredictorBlock similarityPredictorBlock = (SimilarityPredictorBlock) network.getPredictor().getBlock();
                SimilarityProjectorBlock similarityProjectorBlock = (SimilarityProjectorBlock) network.getProjector().getBlock();


                onnxExport(initialInferenceBlock, inputRepresentation, config.getOutputDir() + ONNX + config.getModelName() + "-InitialInference.onnx", "I_");

                onnxExport(recurrentInferenceBlock, inputGeneration, config.getOutputDir() + ONNX + config.getModelName() + "-RecurrentInference.onnx", "R_");

                onnxExport(initialInferenceBlock.getH(), inputRepresentation, config.getOutputDir() + ONNX + config.getModelName() + "-Representation.onnx", "H_");
                onnxExport(initialInferenceBlock.getF(), inputPrediction, config.getOutputDir() + ONNX + config.getModelName() + "-Prediction.onnx", "F_");
                onnxExport(recurrentInferenceBlock.getG(), inputGeneration, config.getOutputDir() + ONNX + config.getModelName() + "-Generation.onnx", "G_");

                onnxExport(similarityPredictorBlock, inputSimilarityPredictor, config.getOutputDir() + ONNX + config.getModelName() + "-SimilarityPredictor.onnx", "SPRE_");
                onnxExport(similarityProjectorBlock, inputSimilarityProjector, config.getOutputDir() + ONNX + config.getModelName() + "-SimilarityProjector.onnx", "SPRO_");
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

    }


}
