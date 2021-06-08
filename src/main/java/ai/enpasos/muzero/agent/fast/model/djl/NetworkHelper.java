/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ai.enpasos.muzero.agent.fast.model.djl;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.djl.training.listener.*;
import ai.djl.training.loss.L2Loss;
import ai.djl.training.loss.SimpleCompositeLoss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.gamebuffer.GameIO;
import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.agent.fast.model.Sample;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.List;

import static ai.enpasos.muzero.MuZero.getNetworksBasedir;
import static ai.enpasos.muzero.agent.fast.model.InputOutputConstruction.constructInput;
import static ai.enpasos.muzero.agent.fast.model.InputOutputConstruction.constructOutput;
import static ai.enpasos.muzero.agent.fast.model.djl.Helper.logNDManagers;


@Slf4j
public class NetworkHelper {


    @SuppressWarnings("ConstantConditions")
    public static int trainAndReturnNumberOfLastTrainingStep(@NotNull MuZeroConfig config, ReplayBuffer replayBuffer, int numberOfEpochs) {
        int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
        int epoch = 0;
        boolean withSymmetryEnrichment = true;
        MuZeroBlock block = new MuZeroBlock(config);

        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {

            logNDManagers(model.getNDManager());

            model.setBlock(block);

            try {
                model.load(Paths.get(getNetworksBasedir(config)));
            } catch (Exception e) {
                log.info("*** no existing model has been found ***");
            }

            String prop = model.getProperty("Epoch");
            if (prop != null) {
                epoch = Integer.parseInt(prop);
            }

            DefaultTrainingConfig djlConfig = setupTrainingConfig(config, epoch);
            try (Trainer trainer = model.newTrainer(djlConfig)) {
                trainer.setMetrics(new Metrics());
                Shape[] inputShapes = getInputShapes(config);
                trainer.initialize(inputShapes);

                for (int i = 0; i < numberOfEpochs; i++) {
                    for (int m = 0; m < numberOfTrainingStepsPerEpoch; m++) {
                        try (Batch batch = getBatch(config, model.getNDManager(), replayBuffer, withSymmetryEnrichment)) {
                            log.debug("trainBatch " + m);
                            EasyTrain.trainBatch(trainer, batch);
                            trainer.step();
                        }
                    }
                    Metrics metrics = trainer.getMetrics();
                    List<Metric> ms = metrics.getMetric("train_all_CompositeLoss");
                    Double meanLoss = ms.stream().mapToDouble(m -> m.getValue().doubleValue()).average().getAsDouble();
                    model.setProperty("MeanLoss", meanLoss.toString());
                    log.info("MeanLoss: " + meanLoss.toString());
                    trainer.notifyListeners(listener -> listener.onEpoch(trainer));
                }

                logNDManagers(trainer.getManager());
            }
        }
        return epoch * numberOfTrainingStepsPerEpoch;
    }


    public static void trainAndReturnNumberOfLastTrainingStep(int numberOfEpochs, @NotNull MuZeroConfig config) {


        ReplayBuffer replayBuffer =
                new ReplayBuffer(config);
        GameIO.readGames(config).forEach(replayBuffer::saveGame);

        trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, numberOfEpochs);
    }

    private static @NotNull Batch getBatch(@NotNull MuZeroConfig config, @NotNull Model model, boolean withSymmetryEnrichment) {
        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        return getBatch(config, model.getNDManager(), replayBuffer, withSymmetryEnrichment);
    }

    private static @NotNull Batch getBatch(@NotNull MuZeroConfig config, @NotNull NDManager ndManager, @NotNull ReplayBuffer replayBuffer, boolean withSymmetryEnrichment) {
        NDManager nd = ndManager.newSubManager();
        List<Sample> batch = replayBuffer.sampleBatch(config.getNumUnrollSteps(), config.getTdSteps(), nd);
        List<NDArray> inputs = constructInput(config, nd, config.getNumUnrollSteps(), batch, withSymmetryEnrichment);
        List<NDArray> outputs = constructOutput(config, nd, config.getNumUnrollSteps(), batch);

        return new Batch(
                nd,
                new NDList(inputs),
                new NDList(outputs),
                (int) inputs.get(0).getShape().get(0),
                null,
                null,
                0,
                0);
    }

    private static Shape @NotNull [] getInputShapes(@NotNull MuZeroConfig conf) {
        return getInputShapes(conf, conf.getBatchSize());
    }

    private static Shape @NotNull [] getInputShapes(@NotNull MuZeroConfig conf, int batchSize) {
        Shape[] shapes = new Shape[conf.getNumUnrollSteps() + 1];
        // for observation input
        shapes[0] = new Shape(batchSize, conf.getNumObservationLayers(), conf.getBoardHeight(), conf.getBoardWidth());
        for (int k = 1; k <= conf.getNumUnrollSteps(); k++) {
            shapes[k] = new Shape(batchSize, 1, conf.getBoardHeight(), conf.getBoardWidth());
        }
        return shapes;
    }


    private static DefaultTrainingConfig setupTrainingConfig(@NotNull MuZeroConfig muZeroConfig, int epoch) {
        String outputDir = getNetworksBasedir(muZeroConfig);
        MyCheckpointsTrainingListener listener = new MyCheckpointsTrainingListener(outputDir);
        listener.setEpoch(epoch);
        SimpleCompositeLoss loss = new SimpleCompositeLoss();

        float gradientScale = 1f / muZeroConfig.getNumUnrollSteps();

        int k = 0;

        // policy
        log.info("k={}: SoftmaxCrossEntropyLoss", k);
        loss.addLoss(new MySoftmaxCrossEntropyLoss("loss_policy_" + k, 1.0f, 1, false, true), k);
        k++;
        // value
        log.info("k={}: L2Loss", k);
        loss.addLoss(new L2Loss("loss_value_" + k, muZeroConfig.getValueLossWeight()), k);
        k++;


        for (int i = 1; i <= muZeroConfig.getNumUnrollSteps(); i++) {
            // policy
            log.info("k={}: SoftmaxCrossEntropyLoss", k);
            loss.addLoss(new MySoftmaxCrossEntropyLoss("loss_policy_" + k, gradientScale, 1, false, true), k);
            k++;
            // value
            log.info("k={}: L2Loss", k);
            loss.addLoss(new L2Loss("loss_value_" + k, muZeroConfig.getValueLossWeight() * gradientScale), k);
            k++;
        }


        return new DefaultTrainingConfig(loss)
                .optOptimizer(setupOptimizer(muZeroConfig))
                .addTrainingListeners(new EpochTrainingListener(),
                        new MemoryTrainingListener(outputDir),
                        new EvaluatorTrainingListener(),
                        new DivergenceCheckTrainingListener(),
                        new MyLoggingTrainingListener(epoch),
                        new TimeMeasureTrainingListener(outputDir))
                .addTrainingListeners(listener);
    }

    private static @NotNull Optimizer setupOptimizer(@NotNull MuZeroConfig muZeroConfig) {

        Tracker learningRateTracker = Tracker.fixed(muZeroConfig.getLrInit());

        return Optimizer.sgd()
                .setLearningRateTracker(learningRateTracker)
                .optMomentum((float) muZeroConfig.getMomentum())
                .optWeightDecays(muZeroConfig.getWeightDecay())
                .optClipGrad(10f)

                .build();


    }


}

