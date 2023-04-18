package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.Device;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.NDList;
import ai.djl.training.Trainer;
import ai.djl.training.evaluator.Evaluator;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.listener.TrainingListenerAdapter;
import ai.djl.training.loss.SimpleCompositeLoss;
import ai.enpasos.muzero.platform.common.Constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link TrainingListener} that records evaluator results.
 *
 * <p>Results are recorded for the following stages:
 *
 * <ul>
 *   <li>{@link Constants#DIR_TRAIN_EPOCH} - This accumulates for the whole player and is recorded to a metric at
 *       the end of the player
 *   <li>{@link Constants#DIR_TRAIN_PROGRESS} - This accumulates for {@link #progressUpdateFrequency} batches and
 *       is recorded to a metric at the end
 *   <li>{@link Constants#DIR_TRAIN_ALL} - This does not accumulates and records every training batch to a metric
 *   <li>{@link Constants#DIR_VALIDATE_EPOCH} - This accumulates for the whole validation player and is recorded
 *       to a metric at the end of the player
 * </ul>
 *
 * <p>The training and validation evaluators are saved as metrics with names that can be found using
 * {@link ai.djl.training.listener.EvaluatorTrainingListener#metricName(Evaluator, String)}. The validation evaluators are
 * also saved as model properties with the evaluator name.
 */
public class MyEvaluatorTrainingListener extends TrainingListenerAdapter {

    private final int progressUpdateFrequency;
    private final Map<String, Float> latestEvaluations;
    private int progressCounter;

    /**
     * Constructs an {@link ai.djl.training.listener.EvaluatorTrainingListener} that updates the training progress the
     * default frequency.
     *
     * <p>Current default frequency is every 5 batches.
     */
    public MyEvaluatorTrainingListener() {
        this(5);
    }

    /**
     * Constructs an {@link ai.djl.training.listener.EvaluatorTrainingListener} that updates the training progress the given
     * frequency.
     *
     * @param progressUpdateFrequency the number of batches to accumulate an evaluator before it is
     *                                stable enough to output
     */
    public MyEvaluatorTrainingListener(int progressUpdateFrequency) {
        this.progressUpdateFrequency = progressUpdateFrequency;
        progressCounter = 0;
        latestEvaluations = new ConcurrentHashMap<>();
    }

    /**
     * Returns the metric created with the evaluator for the given stage.
     *
     * @param evaluator the evaluator to read the metric from
     * @param stage     one of {@link Constants#DIR_TRAIN_EPOCH}, {@link Constants#DIR_TRAIN_PROGRESS}, or {@link Constants#DIR_VALIDATE_EPOCH}
     * @return the metric name to use
     */
    public static String metricName(Evaluator evaluator, String stage) {
        switch (stage) {
            case Constants.DIR_TRAIN_EPOCH:
                return "train_epoch_" + evaluator.getName();
            case Constants.DIR_TRAIN_PROGRESS:
                return "train_progress_" + evaluator.getName();
            case Constants.DIR_TRAIN_ALL:
                return "train_all_" + evaluator.getName();
            case Constants.DIR_VALIDATE_EPOCH:
                return "validate_epoch_" + evaluator.getName();
            default:
                throw new IllegalArgumentException("Invalid metric stage");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEpoch(Trainer trainer) {
        Metrics metrics = trainer.getMetrics();
        for (Evaluator evaluator : trainer.getEvaluators()) {
            float trainValue = evaluator.getAccumulator(Constants.DIR_TRAIN_EPOCH);
            float validateValue = evaluator.getAccumulator(Constants.DIR_VALIDATE_EPOCH);
            if (metrics != null) {
                String key = metricName(evaluator, Constants.DIR_TRAIN_EPOCH);
                metrics.addMetric(key, trainValue);
                String validateKey = metricName(evaluator, Constants.DIR_VALIDATE_EPOCH);
                metrics.addMetric(validateKey, validateValue);
            }

            latestEvaluations.put("train_" + evaluator.getName(), trainValue);
            latestEvaluations.put("validate_" + evaluator.getName(), validateValue);

            if (evaluator == trainer.getLoss()) {
                latestEvaluations.put("train_loss", trainValue);
                latestEvaluations.put("validate_loss", validateValue);
            }
        }
        for (Evaluator evaluator : trainer.getEvaluators()) {
            evaluator.resetAccumulator(Constants.DIR_TRAIN_EPOCH);
            evaluator.resetAccumulator(Constants.DIR_TRAIN_PROGRESS);
            evaluator.resetAccumulator(Constants.DIR_TRAIN_ALL);
            evaluator.resetAccumulator(Constants.DIR_VALIDATE_EPOCH);
        }
        progressCounter = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTrainingBatch(Trainer trainer, BatchData batchData) {
        for (Evaluator evaluator : trainer.getEvaluators()) {
            evaluator.resetAccumulator(Constants.DIR_TRAIN_ALL);
        }

        updateEvaluators(trainer, batchData, new String[]{Constants.DIR_TRAIN_EPOCH, Constants.DIR_TRAIN_PROGRESS, Constants.DIR_TRAIN_ALL});
        Metrics metrics = trainer.getMetrics();
        if (metrics != null) {
            for (Evaluator evaluator : trainer.getEvaluators()) {
                String key = metricName(evaluator, Constants.DIR_TRAIN_ALL);
                float value = evaluator.getAccumulator(Constants.DIR_TRAIN_ALL);
                metrics.addMetric(key, value);
                memorizeLossContributions(metrics, evaluator);
            }

            progressCounter++;
            if (progressCounter == progressUpdateFrequency) {
                for (Evaluator evaluator : trainer.getEvaluators()) {
                    String key = metricName(evaluator, Constants.DIR_TRAIN_PROGRESS);
                    float value = evaluator.getAccumulator(Constants.DIR_TRAIN_PROGRESS);
                    metrics.addMetric(key, value);
                }
                progressCounter = 0;
            }
        }
    }

    private void memorizeLossContributions(Metrics metrics, Evaluator evaluator) {
        if (evaluator instanceof SimpleCompositeLoss simpleCompositeLoss) {
            for (Evaluator evaluatorChild : simpleCompositeLoss.getComponents()) {
                String childKey = metricName(evaluatorChild, Constants.DIR_TRAIN_ALL);
                float childValue = evaluatorChild.getAccumulator(Constants.DIR_TRAIN_ALL);
                metrics.addMetric(childKey, childValue);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onValidationBatch(Trainer trainer, BatchData batchData) {
        updateEvaluators(trainer, batchData, new String[]{Constants.DIR_VALIDATE_EPOCH});
    }

    private void updateEvaluators(Trainer trainer, BatchData batchData, String[] accumulators) {
        for (Evaluator evaluator : trainer.getEvaluators()) {
            for (Device device : batchData.getLabels().keySet()) {
                NDList labels = batchData.getLabels().get(device);
                NDList predictions = batchData.getPredictions().get(device);
                for (String accumulator : accumulators) {
                    evaluator.updateAccumulator(accumulator, labels, predictions);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTrainingBegin(Trainer trainer) {
        for (Evaluator evaluator : trainer.getEvaluators()) {
            evaluator.addAccumulator(Constants.DIR_TRAIN_EPOCH);
            evaluator.addAccumulator(Constants.DIR_TRAIN_PROGRESS);
            evaluator.addAccumulator(Constants.DIR_TRAIN_ALL);
            evaluator.addAccumulator(Constants.DIR_VALIDATE_EPOCH);
        }
    }

    /**
     * Returns the latest evaluations.
     *
     * <p>The latest evaluations are updated on each player.
     *
     * @return the latest evaluations
     */
    public Map<String, Float> getLatestEvaluations() {
        return latestEvaluations;
    }
}
