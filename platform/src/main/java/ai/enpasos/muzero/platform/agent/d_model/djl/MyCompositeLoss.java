package ai.enpasos.muzero.platform.agent.d_model.djl;


import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.training.loss.AbstractCompositeLoss;
import ai.djl.training.loss.IndexLoss;
import ai.djl.training.loss.Loss;
import ai.djl.util.Pair;
import ai.enpasos.muzero.platform.agent.d_model.service.ZipperFunctions;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code MyCompositeLoss} is an implementation of the {@link Loss} abstract class that can
 * combine different {@link Loss} functions by adding the individual losses together.
 *
 * <p>For cases where the losses use only a single index of the labels and/or predictions, use the
 * {@link IndexLoss}.
 *
 * <p>For an example of using this loss, see <a
 * href="https://github.com/deepjavalibrary/djl/blob/master/examples/src/main/java/ai/djl/examples/training/TrainCaptcha.java">the
 * captcha training example.</a>
 */
public class MyCompositeLoss extends AbstractCompositeLoss {

    /**
     * Creates a new empty instance of {@code CompositeLoss} that can combine the given {@link Loss}
     * components.
     */
    public MyCompositeLoss(double rewardLossThreshold, double legalActionLossMaxThreshold) {
        this("CompositeLoss");
        this.legalActionLossMaxThreshold = legalActionLossMaxThreshold;
        this.rewardLossThreshold = rewardLossThreshold;
    }
    double rewardLossThreshold;
    double legalActionLossMaxThreshold;

    /**
     * Creates a new empty instance of {@code CompositeLoss} that can combine the given {@link Loss}
     * components.
     *
     * @param name the display name of the loss
     */
    public MyCompositeLoss(String name) {
        super(name);
        components = new ArrayList<>();
    }

    /**
     * Adds a Loss that applies to all labels and predictions to this composite loss.
     *
     * @param loss the loss to add
     * @return this composite loss
     */
    public MyCompositeLoss addLoss(Loss loss) {
        components.add(loss);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<NDList, NDList> inputForComponent(
            int componentIndex, NDList labels, NDList predictions) {
        return new Pair<>(labels, predictions);
    }




    public NDArray  evaluateWhatToTrain(NDList labels, NDList predictions) {
      //  int symmetryEnhancementFactor = (int)labels.get(0).getShape().get(0)/bOK.length;  // TODO


        NDArray[] lossComponents = new NDArray[components.size()];
        int[] iMap = new int[components.size()];
//        List<NDArray> rewardMasks = new ArrayList<>();
//        List<NDArray> legalActionMasks = new ArrayList<>();
        int sCount = 0;
        for (int i = 0; i < components.size(); i++) {
            Loss loss = ((MyIndexLoss)components.get(i)).getLoss();
            NDList innerLabels = ((MyIndexLoss)components.get(i)).getLabels(labels);
            NDList innerPredictions = ((MyIndexLoss)components.get(i)).getPredictions(predictions);
            if (loss.getName().contains("legal_actions")) {
                lossComponents[i] = ((MyBCELoss) loss).evaluatePartA(innerLabels, innerPredictions);

                lossComponents[i] = lossComponents[i].sum(new int[]{1}, true);  // this is done again in evaluatePartB (could be optimized)
            } else if (loss.getName().contains("reward")) {
                lossComponents[i] = ((MyL2Loss) loss).evaluatePartA(innerLabels, innerPredictions);

            }
            else {
                lossComponents[i] = loss.evaluate(innerLabels, innerPredictions);
            }
        }


        // now the actual masking should happen according to the trainingNeeded
        for (int i = 0; i < components.size(); i++) {
            Loss loss = ((MyIndexLoss) components.get(i)).getLoss();
            if (loss.getName().contains("legal_actions")) {
                lossComponents[i] = ((MyBCELoss) loss).evaluatePartB(lossComponents[i]);
            } else if (loss.getName().contains("reward")) {
                lossComponents[i] = ((MyL2Loss) loss).evaluatePartB(lossComponents[i]);
            }
        }

        if (lossComponents.length == 1) {
            return lossComponents[0];
        } else {
            return NDArrays.add(lossComponents);
        }
    }

}

