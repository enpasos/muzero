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

package ai.enpasos.muzero.network.djl;

import ai.djl.Device;
import ai.djl.Model;
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
import ai.enpasos.muzero.network.Sample;
import ai.enpasos.muzero.network.djl.blocks.atraining.MuZeroBlock;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.util.List;

import static ai.enpasos.muzero.MuZero.getNetworksBasedir;
import static ai.enpasos.muzero.network.InputOutputConstruction.constructInput;
import static ai.enpasos.muzero.network.InputOutputConstruction.constructOutput;


@Slf4j
public class TrainingHelper {


//    public static void loadModelAndForward(MuZeroConfig config) throws Exception {
//
//        ReplayBuffer replayBuffer = new ReplayBuffer(config);
//        GameIO.readGames(config).forEach(replayBuffer::saveGame);
//
//        Game game = replayBuffer.sampleGames().get(0);
//        game.replayToPosition(3);
//
//
//        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {
//            Network network = new Network(config, model);
//            Observation observation = game.getObservation(model.getNDManager());
//            NetworkIO networkOutputFromRepresentation = network.representation(observation);
//
//            NetworkIO networkOutputFromPrediction = network.prediction(networkOutputFromRepresentation);
//
//            Action action = new Action(config, game.getGameDTO().getActionHistory().get(1));
//            networkOutputFromRepresentation.setAction(action);
//            networkOutputFromRepresentation.setConfig(config);
//
//            NetworkIO networkOutputFromDynamics = network.dynamics(networkOutputFromRepresentation);
//        }
//
//    }


    public static int trainAndReturnNumberOfLastTrainingStep(MuZeroConfig config, ReplayBuffer replayBuffer, int numberOfEpochs) {
        int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
        int epoch = 0;
        boolean withSymmetryEnrichment = true;
        MuZeroBlock block = new MuZeroBlock(config);

        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
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
                    trainer.notifyListeners(listener -> listener.onEpoch(trainer));
                }
            }
        }
        return epoch * numberOfTrainingStepsPerEpoch;
    }


//    private static void stateFromOutputToBatch(MuZeroConfig config, NDList output, Batch batch) {
//
//        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {
//            int fromIndex = 0 + 3 * (k - 1);
//            int toIndex = 1 + 2 * (k - 1);
//
//            NDArray from = output.get(fromIndex);
//            batch.getData().set(toIndex, from);
//        }
//
//    }

    public static void trainAndReturnNumberOfLastTrainingStep(int numberOfEpochs, MuZeroConfig config) {


        ReplayBuffer replayBuffer =
                new ReplayBuffer(config);
        GameIO.readGames(config).forEach(replayBuffer::saveGame);

        trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, numberOfEpochs);
    }

    private static Batch getBatch(MuZeroConfig config, Model model, boolean withSymmetryEnrichment) {
        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        return getBatch(config, model.getNDManager(), replayBuffer, withSymmetryEnrichment);
    }

    private static Batch getBatch(MuZeroConfig config, NDManager ndManager, ReplayBuffer replayBuffer, boolean withSymmetryEnrichment) {
        //    log.debug("ndManager.newSubManager()  ... starting");
        NDManager nd = ndManager.newSubManager();
        //   log.debug("ndManager.newSubManager()  ... ended");
        List<Sample> batch = replayBuffer.sampleBatch(config.getNumUnrollSteps(), config.getTdSteps(), nd);
        //     log.debug("replayBuffer.sampleBatch ... done");
        List<NDArray> inputs = constructInput(config, nd, config.getNumUnrollSteps(), batch, withSymmetryEnrichment);
        ///   log.debug("inputs from batch done");
        List<NDArray> outputs = constructOutput(nd, config.getNumUnrollSteps(), batch);
        //     log.debug("outputs from batch done");


        Batch batchDjl = new Batch(
                nd,
                new NDList(inputs),
                new NDList(outputs),
                (int) inputs.get(0).getShape().get(0),
                null,
                null,
                0,
                0);
        return batchDjl;
    }

    private static Shape[] getInputShapes(MuZeroConfig conf) {
        return getInputShapes(conf, conf.getBatchSize());
    }

    private static Shape[] getInputShapes(MuZeroConfig conf, int batchSize) {
        Shape[] shapes = new Shape[conf.getNumUnrollSteps() + 1];
        // for observation input
        shapes[0] = new Shape(batchSize, 3, conf.getBoardHeight(), conf.getBoardWidth());
        for (int k = 1; k <= conf.getNumUnrollSteps(); k++) {
            shapes[k] = new Shape(batchSize, 1, conf.getBoardHeight(), conf.getBoardWidth());
        }
        return shapes;
    }


    private static DefaultTrainingConfig setupTrainingConfig(MuZeroConfig muZeroConfig, int epoch) {
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

        DefaultTrainingConfig config =
                new DefaultTrainingConfig(loss)
                        .optOptimizer(setupOptimizer(muZeroConfig))
                        .addTrainingListeners(new TrainingListener[]{
                                new EpochTrainingListener(),
                                new MemoryTrainingListener(outputDir),
                                new EvaluatorTrainingListener(),
                                new DivergenceCheckTrainingListener(),
                                new MyLoggingTrainingListener(epoch),
                                //   new LoggingTrainingListener(),
                                new TimeMeasureTrainingListener(outputDir)
                        })
                        .addTrainingListeners(listener);


        return config;
    }

    private static Optimizer setupOptimizer(MuZeroConfig muZeroConfig) {

        Tracker learningRateTracker = Tracker.fixed(muZeroConfig.getLrInit());

        return Optimizer.sgd()
                .setLearningRateTracker(learningRateTracker)
                .optMomentum((float) muZeroConfig.getMomentum())
                .optWeightDecays(muZeroConfig.getWeightDecay())
                .optClipGrad(10f)

                .build();


    }


}

