package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.engine.Engine;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.listener.DivergenceCheckTrainingListener;
import ai.djl.training.listener.MemoryTrainingListener;
import ai.djl.training.listener.TimeMeasureTrainingListener;
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
    public static final String LOSS_REWARD = "loss_reward_";
    public static final String LEGAL_ACTIONS_LOSS_VALUE = "loss_legal_actions_";
    public static final String LOSS_SIMILARITY = "loss_similarity_";
    @Autowired
    MuZeroConfig config;



    @Autowired
    MySaveModelTrainingListener mySaveModelTrainingListener;


    public DefaultTrainingConfig setupTrainingConfig(int epoch, boolean saveModel, boolean background, boolean isWithConsistencyLoss, boolean isRulesModel, int numUnrollSteps) {

        String outputDir = config.getNetworkBaseDir();

        MyCompositeLoss loss = new MyCompositeLoss(config.getRewardLossThreshold(), config.getLegalActionLossMaxThreshold());

 //       float gradientScale = isRulesModel ? 1f : 1f / numUnrollSteps;
        float gradientScale =   1f / numUnrollSteps;

        int k = 0;

        //  legal actions
        log.trace("k={}: LegalActions BCELoss", k);
        loss.addLoss(new MyIndexLoss(new MyBCELoss(LEGAL_ACTIONS_LOSS_VALUE + 0, 1f/this.config.getActionSpaceSize(), 1, config.getLegalActionLossMaxThreshold()), k));
        k++;

        // reward
        log.trace("k={}: Reward L2Loss", k);
        loss.addLoss(new MyIndexLoss(new MyL2Loss(LOSS_REWARD + 0, config.getValueLossWeight()  , config.getRewardLossThreshold()), k));
        k++;

        if (!isRulesModel) {
            // policy
            log.trace("k={}: Policy SoftmaxCrossEntropyLoss", k);
            loss.addLoss(new MyIndexLoss(new MySoftmaxCrossEntropyLoss("loss_policy_" + 0, 1.0f, 1, false, true), k));
            k++;

            // value
            log.trace("k={}: Value L2Loss", k);
            loss.addLoss(new MyIndexLoss(new MyL2Loss(LOSS_VALUE + 0, config.getValueLossWeight(),0.0), k));
            k++;
        }


        // switched off gradient scale for legal_action_loss and reward loss
        for (int i = 1; i <= numUnrollSteps; i++) {

            if (isWithConsistencyLoss) {
                // similarity
                log.trace("k={}: Similarity L2Loss", k);
                loss.addLoss(new MyIndexLoss(new MySimilarityLoss(LOSS_SIMILARITY + i, config.getConsistencyLossWeight() * gradientScale), k));
                k++;
            }

            // legal actions
            log.trace("k={}: LegalActions BCELoss", k);
            loss.addLoss(new MyIndexLoss(new MyBCELoss(
                    LEGAL_ACTIONS_LOSS_VALUE + i, 1f / this.config.getActionSpaceSize() * gradientScale, 1, config.getLegalActionLossMaxThreshold()
            ), k));
            k++;

            // reward
            log.trace("k={}: Reward L2Loss", k);
            loss.addLoss(new MyIndexLoss(new MyL2Loss(LOSS_REWARD + i, config.getValueLossWeight() * gradientScale , config.getRewardLossThreshold()), k));
            k++;

            if (!isRulesModel) {
                // policy
                log.trace("k={}: Policy SoftmaxCrossEntropyLoss", k);
                loss.addLoss(new MyIndexLoss(new MySoftmaxCrossEntropyLoss("loss_policy_" + i, gradientScale, 1, false, true), k));
                k++;

                // value
                log.trace("k={}: Value L2Loss", k);
                loss.addLoss(new MyIndexLoss(new MyL2Loss(LOSS_VALUE + i, config.getValueLossWeight() * gradientScale, 0.0), k));
                k++;
            }

        }

        mySaveModelTrainingListener.setOutputDir(outputDir);
        mySaveModelTrainingListener.setEpoch(epoch);

        DefaultTrainingConfig c =  new DefaultTrainingConfig(loss)
                .optDevices(Engine.getInstance().getDevices(1))
                .optOptimizer(setupAdamOptimizer(epoch * config.getNumberOfTrainingStepsPerEpoch(), isRulesModel))
                .addTrainingListeners(
                        new MemoryTrainingListener(outputDir),
                        new MyEvaluatorTrainingListener(),
                        new DivergenceCheckTrainingListener(),
                        new TimeMeasureTrainingListener(outputDir)
                        );
        if (!background) {
            c.addTrainingListeners(
                  //  new MyEpochTrainingListener(),
                    new MyLoggingTrainingListener(epoch) );
        }
        if (saveModel) {
            c.addTrainingListeners(
                    mySaveModelTrainingListener);
        }
        return c;
    }



    private Optimizer setupSGDOptimizer(int trainingStep) {
        float lr = config.getLr(trainingStep);
        log.trace("trainingStep = {}, lr = {}", trainingStep, lr);
        Tracker learningRateTracker = Tracker.fixed(lr);


        return Optimizer.sgd()
                .setLearningRateTracker(learningRateTracker)
                .optWeightDecays(config.getWeightDecay())
                .optClipGrad(1f)
                .build();

    }
    private Optimizer setupAdamOptimizer(int trainingStep, boolean isRulesModel) {
        float lr = config.getLr(trainingStep)  ;
        if (isRulesModel) {
            lr = config.getLrRules();
        }
        log.trace("trainingStep = {}, lr = {}", trainingStep, lr);
        Tracker learningRateTracker = Tracker.fixed(lr);

        return Optimizer.adam()
                .optLearningRateTracker(learningRateTracker)
                .optWeightDecays(config.getWeightDecay())
                .optClipGrad(1f)
                .build();

    }
}
