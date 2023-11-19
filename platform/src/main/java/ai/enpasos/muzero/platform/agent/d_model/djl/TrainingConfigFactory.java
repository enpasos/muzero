package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.engine.Engine;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.listener.DivergenceCheckTrainingListener;
import ai.djl.training.listener.MemoryTrainingListener;
import ai.djl.training.listener.TimeMeasureTrainingListener;
import ai.djl.training.loss.SimpleCompositeLoss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrainingConfigFactory {


    public static final String LOSS_VALUE = "loss_value_";
    public static final String LEGAL_ACTIONS_LOSS_VALUE = "loss_legal_actions_";
    public static final String LOSS_SIMILARITY = "loss_similarity_";
    @Autowired
    MuZeroConfig config;



    @Autowired
    MySaveModelTrainingListener mySaveModelTrainingListener;




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
        if (config.withLegalActionsHead()) {
            log.trace("k={}: LegalActions BCELoss", k);
            loss.addLoss(new MyIndexLoss(new MyBCELoss(LEGAL_ACTIONS_LOSS_VALUE + 0, 1f/config.getActionSpaceSize(), 1), k));
            k++;
        }

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
            if (config.withLegalActionsHead()) {
                log.trace("k={}: LegalActions BCELoss", k);
                loss.addLoss(new MyIndexLoss(new MyBCELoss(LEGAL_ACTIONS_LOSS_VALUE + i,   gradientScale/config.getActionSpaceSize(), 1), k));
                k++;
            }

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
