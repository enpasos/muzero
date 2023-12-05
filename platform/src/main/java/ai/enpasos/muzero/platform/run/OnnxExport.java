package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.agent.d_model.Network;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.InitialInferenceBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.RecurrentInferenceBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.*;
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
    public void run(List<Shape> inputRepresentation, List<Shape> inputAction, int epoch) {

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
                Representation1Block representation1Block = (Representation1Block) network.getRepresentation1().getBlock();
                Representation2Block representation2Block = (Representation2Block) network.getRepresentation2().getBlock();
                DynamicsBlock dynamicsBlock = (DynamicsBlock) network.getDynamics().getBlock();
                LegalActionsBlock legalActionsBlock = (LegalActionsBlock) network.getLegalActions().getBlock();
                RewardBlock rewardBlock = (RewardBlock) network.getReward().getBlock();
                PredictionBlock predictionBlock = (PredictionBlock) network.getPrediction().getBlock();

                SimilarityPredictorBlock similarityPredictorBlock = (SimilarityPredictorBlock) network.getPredictor().getBlock();
                SimilarityProjectorBlock similarityProjectorBlock = (SimilarityProjectorBlock) network.getProjector().getBlock();


                onnxExport(initialInferenceBlock, inputRepresentation, config.getOutputDir() + ONNX + config.getModelName() + "-InitialInference.onnx", "I_");


                Shape[] initialInferenceOutputShapes = initialInferenceBlock.getOutputShapes(new Shape[]  {inputRepresentation.get(0)});

            //    Shape shape = initialInferenceOutputShapes[0];

                List<Shape> state1Shape = List.of(initialInferenceOutputShapes[0]);


                Shape[] h2Shape = initialInferenceBlock.getH2().getOutputShapes(initialInferenceOutputShapes );
                List<Shape> h2ShapeList = List.of( h2Shape[0]);

                onnxExport(recurrentInferenceBlock, state1Shape, config.getOutputDir() + ONNX + config.getModelName() + "-RecurrentInference.onnx", "R_");
//
                onnxExport(representation1Block, inputRepresentation, config.getOutputDir() + ONNX + config.getModelName() + "-Representation1.onnx", "H1_");
                onnxExport(representation2Block, state1Shape, config.getOutputDir() + ONNX + config.getModelName() + "-Representation2.onnx", "H2_");
                onnxExport(predictionBlock, h2ShapeList, config.getOutputDir() + ONNX + config.getModelName() + "-Prediction.onnx", "F_");


              List<Shape> dynamicsInputShapes = List.of(state1Shape.get(0), inputAction.get(0));


               onnxExport(dynamicsBlock, dynamicsInputShapes, config.getOutputDir() + ONNX + config.getModelName() + "-Generation.onnx", "G_");
//

                onnxExport(rewardBlock, dynamicsInputShapes, config.getOutputDir() + ONNX + config.getModelName() + "-Reward.onnx", "F_");
                onnxExport(legalActionsBlock, state1Shape, config.getOutputDir() + ONNX + config.getModelName() + "-LegalActions.onnx", "F_");



                List<Shape> inputSimilarityPredictor = List.of( similarityProjectorBlock.getOutputShapes(state1Shape.toArray(new Shape[0]))[0]);
                onnxExport(similarityPredictorBlock, inputSimilarityPredictor , config.getOutputDir() + ONNX + config.getModelName() + "-SimilarityPredictor.onnx", "SPRE_");
               onnxExport(similarityProjectorBlock, state1Shape, config.getOutputDir() + ONNX + config.getModelName() + "-SimilarityProjector.onnx", "SPRO_");
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

    }


}
