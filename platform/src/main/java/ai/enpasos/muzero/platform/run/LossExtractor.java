package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.atraining.MuZeroBlock;
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
import java.util.Map;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.intuitive.Network.getDoubleValue;
import static ai.enpasos.muzero.platform.agent.intuitive.Network.getEpoch;

@Slf4j
@Component
public class LossExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    NetworkHelper networkHelfer;


    private LossExtractor() {
    }

    @SuppressWarnings("squid:S106")
    public void listLossesForTrainedNetworks() {
        MuZeroBlock block = new MuZeroBlock(config);

        StringWriter stringWriter = new StringWriter();

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';')
            .setHeader("trainingStep"
                , "totalLoss"
                , "valueLoss"
                , "policyLoss"
                , "similarityLoss"

                //  , "actionPaths"
            ).build())) {

            try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
                model.setBlock(block);
                IntStream.range(1, 1000).forEach(
                    i -> {
                        try {
                            model.load(Paths.get(config.getNetworkBaseDir()), model.getName(), Map.of("epoch", i));
                            int epoch = getEpoch(model);
                            int trainingSteps = config.getNumberOfTrainingStepsPerEpoch() * epoch;
                            csvPrinter.printRecord(trainingSteps,
                                NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanLoss")),
                                NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanValueLoss")),
                                NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanPolicyLoss")),
                                NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanSimilarityLoss"))
                                //,
                                //NumberFormat.getNumberInstance().format(getDoubleValue(model, "NumActionPaths"))
                            );
                        } catch (Exception ignored) {
                            log.debug("player " + i + " model.load not successfull");
                        }
                    }
                );
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

        System.out.println(stringWriter);
    }


}
