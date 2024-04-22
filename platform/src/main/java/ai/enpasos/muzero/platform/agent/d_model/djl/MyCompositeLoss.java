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

import java.util.ArrayList;
import java.util.List;

import static ai.djl.ndarray.types.DataType.BOOLEAN;

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
    public MyCompositeLoss() {
        this("CompositeLoss");
    }

//    List<Loss> legalActionLosses;
//    List<Loss> rewardLosses;


    /**
     * Creates a new empty instance of {@code CompositeLoss} that can combine the given {@link Loss}
     * components.
     *
     * @param name the display name of the loss
     */
    public MyCompositeLoss(String name) {
        super(name);
        components = new ArrayList<>();
//        legalActionLosses = new ArrayList<>();
//        rewardLosses = new ArrayList<>();
    }

    /**
     * Adds a Loss that applies to all labels and predictions to this composite loss.
     *
     * @param loss the loss to add
     * @return this composite loss
     */
    public MyCompositeLoss addLoss(Loss loss) {
        components.add(loss);
//        if (loss.getName().contains("legal_actions")) {
//            legalActionLosses.add(loss);
//        }
//        if (loss.getName().contains("reward")) {
//            rewardLosses.add(loss);
//        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<NDList, NDList> inputForComponent(
            int componentIndex, NDList labels, NDList predictions) {
        return new Pair<>(labels, predictions);
    }


    /** {@inheritDoc} */
    @Override
    public NDArray evaluate(NDList labels, NDList predictions) {
        NDArray[] lossComponents = new NDArray[components.size()];
        NDArray[] masks = new NDArray[components.size()];
        List<NDArray> rewardMasks = new ArrayList<>();
        List<NDArray> legalActionMasks = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            Pair<NDList, NDList> inputs = inputForComponent(i, labels, predictions);
            Loss loss = ((MyIndexLoss)components.get(i)).getLoss();
            NDList innerLabels = ((MyIndexLoss)components.get(i)).getLabels(labels);
            NDList innerPredictions = ((MyIndexLoss)components.get(i)).getPredictions(predictions);

            if (loss.getName().contains("legal_actions")) {
                lossComponents[i] = ((MyBCELoss) loss).evaluatePartA(innerLabels, innerPredictions);
                 masks[i] = lossComponents[i].lte(0.3f);

                NDArray intArray = masks[i].toType(DataType.INT32, false);
                NDArray logicalAndResult = intArray.min(new int[]{1}, true);
                masks[i] = logicalAndResult.eq(1);
                legalActionMasks.add(masks[i]);
            } else if (loss.getName().contains("reward")) {
                lossComponents[i] = ((MyL2Loss) loss).evaluatePartA(innerLabels, innerPredictions);
                 masks[i] = lossComponents[i].lte(0.01f);
                rewardMasks.add(masks[i]);
            } else {
                lossComponents[i] = loss.evaluate(innerLabels, innerPredictions);
            }
        }
        List<NDArray> okMasksReward = new ArrayList<>();
        List<NDArray> okMasksLegalActions = new ArrayList<>();


        NDManager manager = legalActionMasks.get(0).getManager();
        NDArray oks = manager.ones(legalActionMasks.get(0).getShape(),  BOOLEAN);
        okMasksReward.add(oks);
        okMasksLegalActions.add(oks);
        for (int i = 0; i < rewardMasks.size() - 1; i++) {
            okMasksReward.add(okMasksReward.get(i).logicalAnd(rewardMasks.get(i)));
            okMasksLegalActions.add(okMasksLegalActions.get(i).logicalAnd(legalActionMasks.get(i)));
        }
        for (int i = 0; i < components.size(); i++) {
            Loss loss = ((MyIndexLoss)components.get(i)).getLoss();
            if (loss.getName().contains("legal_actions")) {
                lossComponents[i].set(okMasksLegalActions.get(i).neg(), 0.0f);
                lossComponents[i] = ((MyBCELoss) loss).evaluatePartB(lossComponents[i]);
            } else if (loss.getName().contains("reward")) {
                lossComponents[i].set(okMasksReward.get(i).neg(), 0.0f);
                lossComponents[i] = ((MyL2Loss) loss).evaluatePartB(lossComponents[i]);
            }
        }
        return NDArrays.add(lossComponents);
    }

}

