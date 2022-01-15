package ai.enpasos.muzero.platform.debug;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.binference.InitialInferenceBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.binference.RecurrentInferenceBlock;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static ai.enpasos.mnist.blocks.OnnxIOExport.onnxExport;
import static ai.enpasos.muzero.platform.agent.fast.model.Network.getDoubleValue;
import static ai.enpasos.muzero.platform.agent.fast.model.Network.getEpoch;

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

            try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
                Network network = new Network(config, model);
                InitialInferenceBlock initialInferenceBlock = (InitialInferenceBlock)network.getInitialInference().getBlock();
                RecurrentInferenceBlock recurrentInferenceBlock = (RecurrentInferenceBlock)network.getRecurrentInference().getBlock();
                onnxExport((OnnxIO) initialInferenceBlock.getH() ,  inputRepresentation, "./models/representation.onnx", "H_");
                onnxExport((OnnxIO) initialInferenceBlock.getF() ,  inputPrediction, "./models/prediction.onnx", "F_");
                onnxExport((OnnxIO)recurrentInferenceBlock.getG() ,  inputGeneration, "./models/generation.onnx", "G_");
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

    }


}
