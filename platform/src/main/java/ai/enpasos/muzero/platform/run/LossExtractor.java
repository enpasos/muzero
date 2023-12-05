package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock;
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

import static ai.enpasos.muzero.platform.agent.d_model.Network.getDoubleValue;
import static ai.enpasos.muzero.platform.agent.d_model.Network.getEpoch;

@Slf4j
@Component
public class LossExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    BatchFactory networkHelfer;


    private LossExtractor() {
    }

    @SuppressWarnings("squid:S106")
    public void listLossesForTrainedNetworks() {
        MuZeroBlock block = new MuZeroBlock(config);

        StringWriter stringWriter = new StringWriter();

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';')
            .setHeader("trainingStep"
                , "totalLoss"
                    , "rewardLoss"
                , "valueLoss"
                    , "legalActionLoss"
                , "policyLoss"
                , "similarityLoss"

                //  , "actionPaths"
            ).build())) {

            extractABlockOfLosses(block, csvPrinter, 1, 500);
            extractABlockOfLosses(block, csvPrinter, 500, 1000);
            extractABlockOfLosses(block, csvPrinter, 1000, 1500);
            extractABlockOfLosses(block, csvPrinter, 1500, 2000);
            extractABlockOfLosses(block, csvPrinter, 2000, 2500);
            extractABlockOfLosses(block, csvPrinter, 2500, 3000);
            extractABlockOfLosses(block, csvPrinter, 3000, 3500);

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

        System.out.println(stringWriter);
    }

    private void extractABlockOfLosses(MuZeroBlock block, CSVPrinter csvPrinter, int start, int end) {

       NumberFormat nf = NumberFormat.getNumberInstance();
       nf.setMaximumFractionDigits(6);
        nf.setMinimumFractionDigits(6);

        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            model.setBlock(block);
            IntStream.range(start, end).forEach(
                i -> {
                    try {
                        model.load(Paths.get(config.getNetworkBaseDir()), model.getName(), Map.of("epoch", i));
                        int epoch = getEpoch(model);
                        int trainingSteps = config.getNumberOfTrainingStepsPerEpoch() * epoch;
                        csvPrinter.printRecord(trainingSteps,
                            NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanLoss")),

                                NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanRewardLoss")),
                                NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanValueLoss")),
                            //    NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanEntropyValueLoss")),

                                NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanLegalActionLoss")),
                            NumberFormat.getNumberInstance().format(getDoubleValue(model, "MeanPolicyLoss")),
                            nf.format(getDoubleValue(model, "MeanSimilarityLoss"))
                            //,

//                            NumberFormat.getNumberInstance().format(getDoubleValue(model, "POLICY_INDEPENDENTMeanLoss")),
//                            NumberFormat.getNumberInstance().format(getDoubleValue(model, "POLICY_INDEPENDENTMeanValueLoss")),
//                            NumberFormat.getNumberInstance().format(getDoubleValue(model, "POLICY_INDEPENDENTMeanPolicyLoss")),
//                            NumberFormat.getNumberInstance().format(getDoubleValue(model, "POLICY_INDEPENDENTMeanSimilarityLoss"))
                            //,
                            //NumberFormat.getNumberInstance().format(getDoubleValue(model, "NumActionPaths"))
                        );
                    } catch (Exception ignored) {
                        log.debug("player " + i + " model.load not successfull");
                    }
                }
            );
        }
    }


}
