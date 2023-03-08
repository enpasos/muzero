package ai.enpasos.muzero.platform.agent.c_model.djl;


import ai.djl.metric.Metrics;
import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListenerAdapter;

/**
 * {@link ai.djl.training.listener.EpochTrainingListener} that tracks epochs.
 *
 * <p>Adds "epoch" metric with epoch times and saves "epoch" model property with numEpochs
 */
public class MyEpochTrainingListener extends TrainingListenerAdapter {

    private long epochTime;
    private int numEpochs;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEpoch(Trainer trainer) {
        Metrics metrics = trainer.getMetrics();
        if (metrics != null) {
            metrics.addMetric("epoch", System.nanoTime() - epochTime);
        }
        epochTime = System.nanoTime();
        numEpochs++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTrainingBegin(Trainer trainer) {
        epochTime = System.nanoTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTrainingEnd(Trainer trainer) {
        trainer.getModel().setProperty("Epoch", Integer.toString(numEpochs));
    }

    /**
     * Returns the number of epochs.
     *
     * @return the number of epochs
     */
    public int getNumEpochs() {
        return numEpochs;
    }

    public void setNumEpochs(int numEpochs) {
        this.numEpochs = numEpochs;
    }
}

