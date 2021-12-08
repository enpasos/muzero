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

package ai.enpasos.muzero.platform.agent.fast.model.djl;


import ai.djl.Device;
import ai.djl.engine.Engine;
import ai.djl.metric.Metrics;
import ai.djl.training.Trainer;
import ai.djl.training.evaluator.Evaluator;
import ai.djl.training.listener.EvaluatorTrainingListener;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.util.ProgressBar;
import ai.enpasos.muzero.platform.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TrainingListener} that outputs the progress of training each batch and epoch into logs.
 *
 * @see <a href="http://docs.djl.ai/docs/development/configure_logging.html">The guide on DJL
 * logging</a>
 */
@SuppressWarnings({"squid:S1192", "squid:S2629"})
public class MyLoggingTrainingListener implements TrainingListener {

    private static final Logger logger = LoggerFactory.getLogger(MyLoggingTrainingListener.class);

    private int frequency;

    private int numEpochs;
    private ProgressBar trainingProgressBar;
    private ProgressBar validateProgressBar;

    /**
     * Constructs a {@code LoggingTrainingListener} instance.
     */
    public MyLoggingTrainingListener(int numEpochs) {
        this.numEpochs = numEpochs;
    }

    /**
     * Constructs a {@code LoggingTrainingListener} instance with specified steps.
     *
     * <p>Print out logs every {@code frequency} epoch.
     *
     * @param frequency the frequency of epoch to print out
     */
    public MyLoggingTrainingListener(int numEpochs, int frequency) {
        this(numEpochs);
        this.frequency = frequency;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEpoch(Trainer trainer) {
        numEpochs++;
        if (frequency > 1 && numEpochs % frequency != 1) {
            return;
        }

        logger.info("Epoch {} finished.", numEpochs);

        Metrics metrics = trainer.getMetrics();
        if (metrics != null) {
            Loss loss = trainer.getLoss();
            String status =
                    getEvaluatorsStatus(
                            metrics,
                            trainer.getEvaluators(),
                            EvaluatorTrainingListener.TRAIN_EPOCH,
                            Short.MAX_VALUE);
            logger.info("Train: {}", status);

            String metricName =
                    EvaluatorTrainingListener.metricName(
                            loss, EvaluatorTrainingListener.VALIDATE_EPOCH);
            if (metrics.hasMetric(metricName)) {
                status =
                        getEvaluatorsStatus(
                                metrics,
                                trainer.getEvaluators(),
                                EvaluatorTrainingListener.VALIDATE_EPOCH,
                                Short.MAX_VALUE);
                if (!status.isEmpty()) {
                    logger.info("Validate: {}", status);
                }
            } else {
                logger.info("validation has not been run.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTrainingBatch(Trainer trainer, BatchData batchData) {
        if (frequency > 1 && numEpochs % frequency != 1) {
            return;
        }

        if (trainingProgressBar == null) {
            trainingProgressBar =
                    new ProgressBar("Training", batchData.getBatch().getProgressTotal());
        }
        trainingProgressBar.update(
                batchData.getBatch().getProgress(),
                getTrainingStatus(trainer, batchData.getBatch().getSize()));
    }

    private String getTrainingStatus(Trainer trainer, int batchSize) {
        Metrics metrics = trainer.getMetrics();
        if (metrics == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(
                getEvaluatorsStatus(
                        metrics,
                        trainer.getEvaluators(),
                        EvaluatorTrainingListener.TRAIN_PROGRESS,
                        2));

        if (metrics.hasMetric(Constants.METRICS_TRAIN)) {
            float batchTime = metrics.latestMetric(Constants.METRICS_TRAIN).getValue().longValue() / 1_000_000_000f;
            sb.append(String.format(", speed: %.2f items/sec", batchSize / batchTime));
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onValidationBatch(Trainer trainer, BatchData batchData) {
        if (frequency > 1 && numEpochs % frequency != 1) {
            return;
        }

        if (validateProgressBar == null) {
            validateProgressBar =
                    new ProgressBar("Validating", batchData.getBatch().getProgressTotal());
        }
        validateProgressBar.update(batchData.getBatch().getProgress());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTrainingBegin(Trainer trainer) {
        String devicesMsg;
        Device[] devices = trainer.getDevices();
        if (devices.length == 1 && Device.Type.CPU.equals(devices[0].getDeviceType())) {
            devicesMsg = Device.cpu().toString();
        } else {
            devicesMsg = devices.length + " GPUs";
        }
        logger.info("Training on: {}.", devicesMsg);

        long init = System.nanoTime();
        Engine engine = trainer.getManager().getEngine();
        String engineName = engine.getEngineName();
        String version = engine.getVersion();
        long loaded = System.nanoTime();
        logger.info(
                String.format(
                        "Load %s Engine Version %s in %.3f ms.",
                        engineName, version, (loaded - init) / 1_000_000f));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTrainingEnd(Trainer trainer) {
        Metrics metrics = trainer.getMetrics();
        if (metrics == null) {
            return;
        }

        float p50;
        float p90;
        if (metrics.hasMetric(Constants.METRICS_TRAIN)) {
            // possible no train metrics if only one iteration is executed
            p50 = metrics.percentile(Constants.METRICS_TRAIN, 50).getValue().longValue() / 1_000_000f;
            p90 = metrics.percentile(Constants.METRICS_TRAIN, 90).getValue().longValue() / 1_000_000f;
            logger.info(String.format(Constants.METRICS_TRAIN + " P50: %.3f ms, P90: %.3f ms", p50, p90));
        }

        if (metrics.hasMetric(Constants.METRICS_FORWARD)) {
            p50 = metrics.percentile(Constants.METRICS_FORWARD, 50).getValue().longValue() / 1_000_000f;
            p90 = metrics.percentile(Constants.METRICS_FORWARD, 90).getValue().longValue() / 1_000_000f;
            logger.info(String.format(Constants.METRICS_FORWARD + " P50: %.3f ms, P90: %.3f ms", p50, p90));
        }

        if (metrics.hasMetric(Constants.METRICS_TRAINING_METRICS)) {
            p50 = metrics.percentile(Constants.METRICS_TRAINING_METRICS, 50).getValue().longValue() / 1_000_000f;
            p90 = metrics.percentile(Constants.METRICS_TRAINING_METRICS, 90).getValue().longValue() / 1_000_000f;
            logger.info(String.format(Constants.METRICS_TRAINING_METRICS + " P50: %.3f ms, P90: %.3f ms", p50, p90));
        }

        if (metrics.hasMetric(Constants.METRICS_BACKWARD)) {
            p50 = metrics.percentile(Constants.METRICS_BACKWARD, 50).getValue().longValue() / 1_000_000f;
            p90 = metrics.percentile(Constants.METRICS_BACKWARD, 90).getValue().longValue() / 1_000_000f;
            logger.info(String.format(Constants.METRICS_BACKWARD + " P50: %.3f ms, P90: %.3f ms", p50, p90));
        }

        if (metrics.hasMetric("step")) {
            p50 = metrics.percentile("step", 50).getValue().longValue() / 1_000_000f;
            p90 = metrics.percentile("step", 90).getValue().longValue() / 1_000_000f;
            logger.info(String.format("step P50: %.3f ms, P90: %.3f ms", p50, p90));
        }

        if (metrics.hasMetric(Constants.EPOCH)) {
            p50 = metrics.percentile(Constants.EPOCH, 50).getValue().longValue() / 1_000_000_000f;
            p90 = metrics.percentile(Constants.EPOCH, 90).getValue().longValue() / 1_000_000_000f;
            logger.info(String.format(Constants.EPOCH + " P50: %.3f s, P90: %.3f s", p50, p90));
        }
    }

    private String getEvaluatorsStatus(
            Metrics metrics, List<Evaluator> toOutput, String stage, int limit) {
        List<String> metricOutputs = new ArrayList<>(limit + 1);
        int count = 0;
        for (Evaluator evaluator : toOutput) {
            if (++count > limit) {
                metricOutputs.add("...");
                break;
            }
            String metricName = EvaluatorTrainingListener.metricName(evaluator, stage);
            if (metrics.hasMetric(metricName)) {
                float value = metrics.latestMetric(metricName).getValue().floatValue();
                // use .2 precision to avoid new line in progress bar

                if (Math.abs(value) < .01 || Math.abs(value) > 9999) {
                    metricOutputs.add(String.format("%s: %.2E", evaluator.getName(), value));
                } else if (!(metricName.startsWith("validate_") && Float.isNaN(value))) {
                    metricOutputs.add(String.format("%s: %.2f", evaluator.getName(), value));
                }
            } else {
                metricOutputs.add(String.format("%s: _", evaluator.getName()));
            }
        }
        return String.join(", ", metricOutputs);
    }
}
