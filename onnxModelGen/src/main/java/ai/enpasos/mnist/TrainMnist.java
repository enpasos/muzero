package ai.enpasos.mnist;

/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */


import ai.djl.Model;
import ai.djl.basicdataset.cv.classification.Mnist;
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
import ai.djl.training.listener.SaveModelTrainingListener;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.enpasos.mnist.blocks.MnistBlock;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxIOExport.onnxExport;

/**
 * An example of training an image classification (MNIST) model.
 *
 * <p>See this <a
 * href="https://github.com/deepjavalibrary/djl/blob/master/examples/docs/train_mnist_mlp.md">doc</a>
 * for information about this example.
 */
@Slf4j
public final class TrainMnist {

    private TrainMnist() {
    }

    public static void main(String[] args) throws IOException, TranslateException {
        String[] args_ = {"-e", "12", "-b", "256", "-o", "mymodel"};
        TrainMnist.runExample(args_);
    }

    public static TrainingResult runExample(String[] args) throws IOException, TranslateException {
        Arguments arguments = new Arguments().parseArgs(args);
        if (arguments == null) {
            return null;
        }

        // Construct neural network
        Block block = MnistBlock.newMnistBlock();

        try (Model model = Model.newInstance("mymodel")) {
            model.setBlock(block);

            // get training and validation dataset
            RandomAccessDataset trainingSet = getDataset(Dataset.Usage.TRAIN, arguments);
            RandomAccessDataset validateSet = getDataset(Dataset.Usage.TEST, arguments);

            // setup training configuration
            DefaultTrainingConfig config = setupTrainingConfig(arguments);

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.setMetrics(new Metrics());

                /*
                 * MNIST is 28x28 grayscale image and pre processed into 28 * 28 NDArray.
                 * 1st axis is batch axis, we can use 1 for initialization.
                 */
                Shape inputShape = new Shape(1, 1, Mnist.IMAGE_HEIGHT, Mnist.IMAGE_WIDTH);

                trainer.initialize(inputShape);

                EasyTrain.fit(trainer, arguments.getEpoch(), trainingSet, validateSet);

                onnxExport(model, List.of(inputShape), "./models/mnist.onnx");

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
                });
        return new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
                .addEvaluator(new Accuracy())
                .optDevices(Engine.getInstance().getDevices(arguments.getMaxGpus()))
                .addTrainingListeners(TrainingListener.Defaults.logging(outputDir))
                .addTrainingListeners(listener);
    }

    private static RandomAccessDataset getDataset(Dataset.Usage usage, Arguments arguments)
            throws IOException {
        Mnist mnist =
                Mnist.builder()
                        .optUsage(usage)
                        .setSampling(arguments.getBatchSize(), true)
                        .optLimit(arguments.getLimit())
                        .build();
        mnist.prepare(new ProgressBar());
        return mnist;
    }
}
