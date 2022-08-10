package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.memorize.Game;
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
import java.util.Optional;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.intuitive.Network.getDoubleValue;
import static ai.enpasos.muzero.platform.agent.intuitive.Network.getEpoch;

@Slf4j
@Component
public class CycleExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    NetworkHelper networkHelfer;


    private CycleExtractor() {
    }

    @SuppressWarnings("squid:S106")
    public void run(int[] actions, int lastAction ) {

        Game game = this.getGameStartingWithActionsFromStart(actions);

        MuZeroBlock block = new MuZeroBlock(config);

        StringWriter stringWriter = new StringWriter();


        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';').setHeader("trainingStep", "probability").build())) {
            IntStream.range(1, 1000).forEach(
                i -> {
                    try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
                        model.setBlock(block);


                        model.load(Paths.get(config.getNetworkBaseDir()), model.getName(), Map.of("epoch", i));
                        int epoch = getEpoch(model);
                        double p = getProbability(model, game, lastAction);
                        int trainingSteps = config.getNumberOfTrainingStepsPerEpoch() * epoch;
                        csvPrinter.printRecord(trainingSteps,
                            NumberFormat.getNumberInstance().format(p)
                        );

                    } catch (Exception ignored) {
                        log.debug("player " + i + " model.load not successfull");
                    }
                }
                );


        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

        System.out.println(stringWriter);
    }

    private double getProbability(Model model, Game game, int action) {
        Network network = new Network(config, model);
        try (NDManager nDManager = network.getNDManager().newSubManager()) {

            network.initActionSpaceOnDevice(nDManager);
            network.setHiddenStateNDManager(nDManager);


            List<NetworkIO> networkOutput = network.initialInferenceListDirect(List.of(game));


            float[] policyValues = networkOutput.get(0).getPolicyValues();
            return (double) policyValues[action];
        }



    }

    public Game getGameStartingWithActionsFromStart(int... actions) {

        Game game = this.config.newGame();
        game.apply(actions);

        return game;

    }
}
