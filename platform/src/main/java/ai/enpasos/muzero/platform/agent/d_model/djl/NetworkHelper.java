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

package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.dataset.Batch;
import ai.djl.training.listener.DivergenceCheckTrainingListener;
import ai.djl.training.listener.MemoryTrainingListener;
import ai.djl.training.listener.TimeMeasureTrainingListener;
import ai.djl.training.loss.SimpleCompositeLoss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import ai.enpasos.muzero.platform.agent.d_model.InputOutputConstruction;
import ai.enpasos.muzero.platform.agent.d_model.Sample;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;


@Slf4j
@Component
public class NetworkHelper {

    public static final String LOSS_VALUE = "loss_value_";
    public static final String ENTROPY_LOSS_VALUE = "entropy_loss_value_";
    public static final String LOSS_SIMILARITY = "loss_similarity_";
    @Autowired
    MuZeroConfig config;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    InputOutputConstruction inputOutputConstruction;

    @Autowired
    MySaveModelTrainingListener mySaveModelTrainingListener;

    public static int getEpochFromModel(Model model) {
        int epoch = 0;
        String prop = model.getProperty("Epoch");
        if (prop != null) {
            epoch = Integer.parseInt(prop);
        }
        return epoch;
    }

    public int getEpoch() {

        int epoch = 0;

        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {

            if (model.getBlock() == null) {
                MuZeroBlock block = new MuZeroBlock(config);
                model.setBlock(block);
                try {
                    model.load(Paths.get(config.getNetworkBaseDir()));
                } catch (Exception e) {
                    log.info("*** no existing model has been found ***");
                }
            }


            epoch = getEpochFromModel(model);


        }
        return epoch;
    }

//    public int getEpoch(Model model) {
//
//        if (model.getBlock() == null) {
//            MuZeroBlock block = new MuZeroBlock(config);
//            model.setBlock(block);
//            try {
//                model.load(Paths.get(config.getNetworkBaseDir()));
//            } catch (Exception e) {
//                log.info("*** no existing model has been found ***");
//            }
//        }
//
//
//        return getEpochFromModel(model);
//    }


    public Batch getBatch(@NotNull NDManager ndManager, boolean withSymmetryEnrichment) {
        List<Sample> batch = gameBuffer.sampleBatch(config.getNumUnrollSteps());

        NDManager nd = ndManager.newSubManager();

        List<NDArray> inputs = inputOutputConstruction.constructInput(nd, config.getNumUnrollSteps(), batch, withSymmetryEnrichment);


        List<NDArray> outputs = inputOutputConstruction.constructOutput(nd, config.getNumUnrollSteps(), batch);

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

    public Shape @NotNull [] getInputShapes() {
        return getInputShapes(config.getBatchSize());
    }

    public Shape @NotNull [] getInputShapes(int batchSize) {
        Shape[] shapes = new Shape[config.getNumUnrollSteps() + 1];
        // for observation input
        shapes[0] = new Shape(batchSize, config.getNumObservationLayers(), config.getBoardHeight(), config.getBoardWidth());
        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {
            shapes[k] = new Shape(batchSize, config.getNumActionLayers(), config.getBoardHeight(), config.getBoardWidth());
        }
        return shapes;
    }


    public DefaultTrainingConfig setupTrainingConfig(int epoch) {
        String outputDir = config.getNetworkBaseDir();
        MySaveModelTrainingListener listener = mySaveModelTrainingListener;
        mySaveModelTrainingListener.setOutputDir(outputDir);
        listener.setEpoch(epoch);
        SimpleCompositeLoss loss = new SimpleCompositeLoss();

        config.getValueSpan();

        float gradientScale = 1f / config.getNumUnrollSteps();

        int k = 0;

        // policy
        log.trace("k={}: Policy SoftmaxCrossEntropyLoss", k);
        loss.addLoss(new MyIndexLoss(new MySoftmaxCrossEntropyLoss("loss_policy_" + 0, 1.0f, 1, false, true), k));
        k++;

        // value
        log.trace("k={}: Value L2Loss", k);
        loss.addLoss(new MyIndexLoss(new MyL2Loss(LOSS_VALUE + 0, config.getValueLossWeight()), k));
        k++;

        // entropyValue
        log.trace("k={}: EntropyValue L2Loss", k);
        loss.addLoss(new MyIndexLoss(new MyL2Loss(ENTROPY_LOSS_VALUE + 0, config.getEntropyValueLossWeight()), k));
        k++;


        for (int i = 1; i <= config.getNumUnrollSteps(); i++) {
            // policy
            log.trace("k={}: Policy SoftmaxCrossEntropyLoss", k);
            loss.addLoss(new MyIndexLoss(new MySoftmaxCrossEntropyLoss("loss_policy_" + i, gradientScale, 1, false, true), k));
            k++;

            // value
            log.trace("k={}: Value L2Loss", k);
            loss.addLoss(new MyIndexLoss(new MyL2Loss(LOSS_VALUE + i, config.getValueLossWeight() * gradientScale), k));
            k++;

            // entropyValue
            log.trace("k={}: EntropyValue L2Loss", k);
            loss.addLoss(new MyIndexLoss(new MyL2Loss(ENTROPY_LOSS_VALUE + i, config.getEntropyValueLossWeight() * gradientScale), k));
            k++;

            // similarity
            log.trace("k={}: Similarity L2Loss", k);
            loss.addLoss(new MyIndexLoss(new MySimilarityLoss(LOSS_SIMILARITY + i, 2 * gradientScale), k));

            k++;
        }


        return new DefaultTrainingConfig(loss)
            .optDevices(Engine.getInstance().getDevices(1))
            .optOptimizer(setupOptimizer())
            .addTrainingListeners(
                new MyEpochTrainingListener(),
                new MemoryTrainingListener(outputDir),
                new MyEvaluatorTrainingListener(),
                new DivergenceCheckTrainingListener(),
                new MyLoggingTrainingListener(epoch),
                new TimeMeasureTrainingListener(outputDir),
                listener);
    }

    private Optimizer setupOptimizer() {

        Tracker learningRateTracker = Tracker.fixed(config.getLrInit());

        return Optimizer.adam()
            .optLearningRateTracker(learningRateTracker)
            .optWeightDecays(config.getWeightDecay())
            .optClipGrad(1f)
            .build();

    }


}

