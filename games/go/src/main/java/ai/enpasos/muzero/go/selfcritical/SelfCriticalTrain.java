package ai.enpasos.muzero.go.selfcritical;


import ai.djl.Model;
import ai.djl.basicmodelzoo.basic.Mlp;
import ai.djl.engine.Engine;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingResult;
import ai.djl.training.dataset.Dataset;
import ai.djl.training.dataset.RandomAccessDataset;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.listener.*;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.enpasos.mnist.Arguments;
import ai.enpasos.muzero.platform.agent.fast.model.djl.MyEvaluatorTrainingListener;
import ai.enpasos.muzero.platform.agent.fast.model.djl.MyLoggingTrainingListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class SelfCriticalTrain {

    private SelfCriticalTrain() {}

    public TrainingResult run(SelfCriticalDataSet dataSet) throws IOException, TranslateException {
        String[] args_ = {"-e", "3", "-b", "256", "-o", "mymodel"};

        Arguments arguments = new Arguments().parseArgs(args_);
        if (arguments == null) {
            return null;
        }

        // Construct neural network
        Block block = SelfCriticalBlock.newSelfCriticalBlock();

        try (Model model = Model.newInstance("mlp")) {
            model.setBlock(block);

            // get training and validation dataset
            RandomAccessDataset trainingSet = getDataset(Dataset.Usage.TRAIN, arguments, dataSet.getTrainingDataSet());
            RandomAccessDataset validateSet = getDataset(Dataset.Usage.TEST, arguments, dataSet.getTestDataSet());

            // setup training configuration
            DefaultTrainingConfig config = setupTrainingConfig(arguments);

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.setMetrics(new Metrics());

                Shape inputShape = new Shape(1, 3);

                // initialize trainer with proper input shape
                trainer.initialize(inputShape);

                EasyTrain.fit(trainer, arguments.getEpoch(), trainingSet, validateSet);

                return trainer.getTrainingResult();
            }
        }
    }

    private static DefaultTrainingConfig setupTrainingConfig(Arguments arguments) {


        String outputDir = arguments.getOutputDir();
        SaveModelTrainingListener listener = new SaveModelTrainingListener(outputDir);
        listener.setSaveModelCallback(
                trainer -> {
                    TrainingResult result = trainer.getTrainingResult();
                    Model model = trainer.getModel();
                    float accuracy = result.getValidateEvaluation("Accuracy");
                    model.setProperty("Accuracy", String.format("%.4f", accuracy));
                    model.setProperty("Loss", String.format("%.4f", result.getValidateLoss()));
                    log.info("Accuracy: " + String.format("%.4f", accuracy));
                    log.info("Loss: " + String.format("%.4f", result.getValidateLoss()));
                });
        return new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
                .addEvaluator(new Accuracy())
                .optDevices(Engine.getInstance().getDevices(arguments.getMaxGpus()))
                .addTrainingListeners(TrainingListener.Defaults.logging(outputDir))
            .optOptimizer(setupOptimizer())
                .addTrainingListeners(listener);

    }

    private static Optimizer setupOptimizer() {

        Tracker learningRateTracker = Tracker.fixed(0.0001f);

        return Optimizer.adam()
            .optLearningRateTracker(learningRateTracker)
           // .optWeightDecays(config.getWeightDecay())
            .optClipGrad(10f)
            .build();
    }


    private static DJLDataSet getDataset(Dataset.Usage usage, Arguments arguments, SelfCriticalDataSet inputData)
        throws IOException {
        DJLDataSet dataSet =   DJLDataSet.builder()
                .optUsage(usage)
                .setSampling(arguments.getBatchSize(), true)
                .optLimit(Long.getLong("DATASET_LIMIT", Long.MAX_VALUE))
                .inputData(inputData)
                .build();
        dataSet.prepare(new ProgressBar());
        return dataSet;
    }
}
