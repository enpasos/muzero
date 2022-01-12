package ai.enpasos.muzero.platform.debug;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
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
    public void run() {
        MuZeroBlock block = new MuZeroBlock(config);

        StringWriter stringWriter = new StringWriter();

// Beispiel tictactoe
        List<Shape> inputInitialInference = List.of(new Shape(1L,3L,3L,3L));
        List<Shape> inputRecurrentInference = List.of(new Shape(1L,5L,3L,3L), new Shape(1L,1L,3L,3L));

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.withDelimiter(';').withHeader("trainingStep", "totalLoss", "valueLoss", "policyLoss"))) {

            try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
                Network network = new Network(config, model);
                onnxExport((OnnxIO) network.getInitialInference().getBlock(),  inputInitialInference, "./models/initialInference.onnx");
                onnxExport((OnnxIO)network.getRecurrentInference().getBlock(),  inputRecurrentInference, "./models/recurrentInference.onnx");
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

    }


}
