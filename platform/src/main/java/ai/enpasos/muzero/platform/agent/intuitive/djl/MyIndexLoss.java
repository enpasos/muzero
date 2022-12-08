package ai.enpasos.muzero.platform.agent.intuitive.djl;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.training.loss.Loss;

/**
 * A wrapper for a {@link Loss} that evaluates on only a particular {@link NDArray} in the
 * predictions and/or labels {@link NDList}s.
 */
public class MyIndexLoss extends Loss {

    private Loss loss;
    private Integer predictionsIndex;
    private Integer labelsIndex;

    /**
     * Constructs an {@link ai.djl.training.loss.IndexLoss} with the same index for both predictions and labels.
     *
     * @param loss the base evaluator
     * @param index the index for both predictions and labels
     */
    public MyIndexLoss(Loss loss, int index) {
        this(loss, index, index);
    }

    /**
     * Constructs an {@link ai.djl.training.loss.IndexLoss}.
     *
     * @param loss the base evaluator
     * @param predictionsIndex the predictions index
     * @param labelsIndex the labels index
     */
    public MyIndexLoss(Loss loss, Integer predictionsIndex, Integer labelsIndex) {
        super(loss.getName());
        this.loss = loss;
        this.predictionsIndex = predictionsIndex;
        this.labelsIndex = labelsIndex;
    }

    /** {@inheritDoc} */
    @Override
    public NDArray evaluate(NDList labels, NDList predictions) {
        return loss.evaluate(getLabels(labels), getPredictions(predictions));
    }

    private NDList getPredictions(NDList predictions) {
        if (predictionsIndex == null) {
            return predictions;
        }
        return new NDList(predictions.get(predictionsIndex));
    }

    private NDList getLabels(NDList labels) {
        if (labelsIndex == null) {
            return labels;
        }
        return new NDList(labels.get(labelsIndex));
    }

    public Loss getLoss() {
        return loss;
    }
}
