package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference.InitialInferenceBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference.RecurrentInferenceBlock;
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

    @Autowired
    MuZeroConfig config;

    @Autowired
    NetworkHelper networkHelfer;


    private OnnxExport() {
    }

    @SuppressWarnings("squid:S106")
    public void run(List<Shape> inputRepresentation, List<Shape> inputPrediction, List<Shape> inputGeneration) {
        MuZeroBlock block = new MuZeroBlock(config);

        StringWriter stringWriter = new StringWriter();


        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.withDelimiter(';').withHeader("trainingStep", "totalLoss", "valueLoss", "policyLoss"))) {
            FileUtils.forceMkdir(new File(config.getOutputDir()+"onnx"));
            try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
                Network network = new Network(config, model);
                InitialInferenceBlock initialInferenceBlock = (InitialInferenceBlock)network.getInitialInference().getBlock();
                RecurrentInferenceBlock recurrentInferenceBlock = (RecurrentInferenceBlock)network.getRecurrentInference().getBlock();

                onnxExport((OnnxIO) initialInferenceBlock ,  inputRepresentation, config.getOutputDir()+"onnx/" + config.getModelName() + "-InitialInference.onnx", "I_");

                onnxExport((OnnxIO) recurrentInferenceBlock ,  inputGeneration, config.getOutputDir()+"onnx/" + config.getModelName() + "-RecurrentInference.onnx", "R_");

                onnxExport((OnnxIO) initialInferenceBlock.getH() ,  inputRepresentation, config.getOutputDir()+"onnx/"  + config.getModelName() + "-Representation.onnx", "H_");
                onnxExport((OnnxIO) initialInferenceBlock.getF() ,  inputPrediction, config.getOutputDir()+"onnx/"+ config.getModelName() +"-Prediction.onnx", "F_");
                onnxExport((OnnxIO)recurrentInferenceBlock.getG() ,  inputGeneration, config.getOutputDir()+"onnx/"+ config.getModelName() +"-Generation.onnx", "G_");
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

    }


}
