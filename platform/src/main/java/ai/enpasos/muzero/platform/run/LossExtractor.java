package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.e_experience.NetworkIOService;
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
import java.util.Arrays;
import java.util.Map;

import static ai.enpasos.muzero.platform.agent.d_model.Network.getDoubleValue;
import static ai.enpasos.muzero.platform.agent.d_model.Network.getEpoch;

@Slf4j
@Component
public class LossExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    BatchFactory networkHelfer;

    @Autowired
    NetworkIOService networkIOService;


    private LossExtractor() {
    }

    public void listLossesForTrainedNetworks() {
        listLossesForTrainedNetworks(1);
    }

    @SuppressWarnings("squid:S106")
    public void listLossesForTrainedNetworks(int startingEpoch) {
        MuZeroBlock block = new MuZeroBlock(config);

        StringWriter stringWriter = new StringWriter();

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';')
            .setHeader("trainingStep"
                , "totalLoss"
                    , "legalActionLoss"
                    , "rewardLoss"
                    , "similarityLoss"
                    , "policyLoss"
                , "valueLoss"
            ).build())) {


            int[] epochs = networkIOService.getNetworkEpochs();

            extractLosses(block, csvPrinter, epochs, startingEpoch);

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

        System.out.println(stringWriter);
    }

    private int[] extractEpochs() {
        return null;
    }

    private void extractLosses(MuZeroBlock block, CSVPrinter csvPrinter, int[] epochs, int startingEpoch) {

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(8);


        Arrays.stream(epochs).forEach(
                epoch -> {
                    if (epoch < startingEpoch) return;
                    log.info("epoch = {}", epoch);
                    try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
                        model.setBlock(block);
                        model.load(Paths.get(config.getNetworkBaseDir()), model.getName(), Map.of("epoch", epoch));

                        int trainingSteps = config.getNumberOfTrainingStepsPerEpoch() * epoch;
                        csvPrinter.printRecord(trainingSteps,
                                NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanLoss")),
                                nf.format(getDoubleValue(model, "MeanLegalActionLoss")),
                                nf.format(getDoubleValue(model, "MeanRewardLoss")),
                                nf.format(getDoubleValue(model, "MeanSimilarityLoss")),
                                nf.format(getDoubleValue(model, "MeanPolicyLoss")),
                                nf.format(getDoubleValue(model, "MeanValueLoss"))

                        );
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                }
        );

    }


}
