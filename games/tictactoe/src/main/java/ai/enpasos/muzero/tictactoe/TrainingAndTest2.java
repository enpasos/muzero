package ai.enpasos.muzero.tictactoe;


import ai.djl.Device;
import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.BaseNDManager;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.MuZero;
import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.djl.MyCheckpointsTrainingListener;
import ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.tictactoe.config.TicTacToeConfigFactory;
import ai.enpasos.muzero.tictactoe.test.TicTacToeTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import static ai.enpasos.muzero.platform.MuZero.getNetworksBasedir;
import static ai.enpasos.muzero.platform.MuZero.train;
import static ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper.*;

@Slf4j
public class TrainingAndTest2 {

    public static void main(String[] args) throws URISyntaxException, IOException {
        MuZeroConfig config = TicTacToeConfigFactory.getTicTacToeInstance();

        String dir = "./memory/";
        config.setOutputDir(dir);

        boolean freshBuffer = false;
        int numberOfEpochs = 1;

        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            Network network = new Network(config, model);

            FileUtils.deleteDirectory(new File(dir));
            MuZero.createNetworkModelIfNotExisting(config);


            ReplayBuffer replayBuffer = new ReplayBuffer(config);
            if (freshBuffer) {
                while (!replayBuffer.getBuffer().isBufferFilled()) {
                    network.debugDump();
                    MuZero.playOnDeepThinking(network, replayBuffer);
                    replayBuffer.saveState();
                }
            } else {
                replayBuffer.loadLatestState();
                MuZero.initialFillingBuffer(network, replayBuffer);
            }

            int trainingStep = NetworkHelper.numberOfLastTrainingStep(config);
            DefaultTrainingConfig djlConfig = setupTrainingConfig(config, 0);

            while (trainingStep < config.getNumberOfTrainingSteps()) {
                if (trainingStep != 0) {
                    log.info("last training step = {}", trainingStep);
                    log.info("numSimulations: " + config.getNumSimulations());
                    network.debugDump();
                    MuZero.playOnDeepThinking(network, replayBuffer);
                    replayBuffer.saveState();
                }
                network.debugDump();
                int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
                int epoch = 0;
                boolean withSymmetryEnrichment = true;

                String prop = model.getProperty("Epoch");
                if (prop != null) {
                    epoch = Integer.parseInt(prop);
                }

                int finalEpoch = epoch;
                djlConfig.getTrainingListeners().stream()
                        .filter(trainingListener -> trainingListener instanceof MyCheckpointsTrainingListener)
                        .forEach(trainingListener -> ((MyCheckpointsTrainingListener) trainingListener).setEpoch(finalEpoch));

                try (Trainer trainer = model.newTrainer(djlConfig)) {
                    network.debugDump();
                    trainer.setMetrics(new Metrics());
                    Shape[] inputShapes = getInputShapes(config);
                    trainer.initialize(inputShapes);

                    for (int i = 0; i < numberOfEpochs; i++) {
                        for (int m = 0; m < numberOfTrainingStepsPerEpoch; m++) {
                            try (Batch batch = getBatch(config, trainer.getManager(), replayBuffer, withSymmetryEnrichment)) {
                                log.debug("trainBatch " + m);
                                EasyTrain.trainBatch(trainer, batch);
                                trainer.step();
                            }
                            network.debugDump();
                        }
                        Metrics metrics = trainer.getMetrics();

                        // mean loss
                        List<Metric> ms = metrics.getMetric("train_all_CompositeLoss");
                        Double meanLoss = ms.stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble();
                        model.setProperty("MeanLoss", meanLoss.toString());
                        log.info("MeanLoss: " + meanLoss.toString());

                        // mean value loss
                        Double meanValueLoss = metrics.getMetricNames().stream()
                                .filter(name -> name.startsWith("train_all") && name.contains("value_0"))
                                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble())
                                .sum();
                        meanValueLoss += metrics.getMetricNames().stream()
                                .filter(name -> name.startsWith("train_all") && !name.contains("value_0") && name.contains("value"))
                                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble())
                                .sum();
                        model.setProperty("MeanValueLoss", meanValueLoss.toString());
                        log.info("MeanValueLoss: " + meanValueLoss.toString());

                        // mean policy loss
                        Double meanPolicyLoss = metrics.getMetricNames().stream()
                                .filter(name -> name.startsWith("train_all") && name.contains("policy_0"))
                                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble())
                                .sum();
                        meanPolicyLoss += metrics.getMetricNames().stream()
                                .filter(name -> name.startsWith("train_all") && !name.contains("policy_0") && name.contains("policy"))
                                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble())
                                .sum();
                        model.setProperty("MeanPolicyLoss", meanPolicyLoss.toString());
                        log.info("MeanPolicyLoss: " + meanPolicyLoss.toString());

                        network.debugDump();

                        trainer.notifyListeners(listener -> {
                            System.out.println(listener.toString());
                            network.debugDump();
                            listener.onEpoch(trainer);
                            network.debugDump();
                        });

                        network.debugDump();
                    }

//                    network.debugDump();
//               //     trainer.close();
//                    trainer.notifyListeners((listener) -> {
//                        System.out.println(listener.toString());
//                        network.debugDump();
//                        listener.onTrainingEnd(trainer);
//                        network.debugDump();
//                    });
////                    network.debugDump();
////                    trainer.parameterStore.sync();
//                    network.debugDump();
//                    trainer.getManager().close();
//                    network.debugDump();
//                    trainer.close();
//                    network.debugDump();
                }
                network.debugDump();
                trainingStep = epoch * numberOfTrainingStepsPerEpoch;

            }

            boolean passed = TicTacToeTest.test(config);
            String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
            log.info(message);
            if (!passed) throw new RuntimeException(message);
        }
    }


}
