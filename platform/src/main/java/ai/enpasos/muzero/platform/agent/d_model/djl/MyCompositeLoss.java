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
import ai.enpasos.muzero.platform.common.MuZeroException;

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




    public Pair<NDArray, NDArray> evaluateWithReturn(NDList labels, NDList predictions) {
        NDArray[] lossComponents = new NDArray[components.size()];
        int[] iMap = new int[components.size()];
        List<NDArray> rewardMasks = new ArrayList<>();
        List<NDArray> legalActionMasks = new ArrayList<>();
        int sCount = 0;
        for (int i = 0; i < components.size(); i++) {
           // Pair<NDList, NDList> inputs = inputForComponent(i, labels, predictions);
            Loss loss = ((MyIndexLoss)components.get(i)).getLoss();
            NDList innerLabels = ((MyIndexLoss)components.get(i)).getLabels(labels);
            NDList innerPredictions = ((MyIndexLoss)components.get(i)).getPredictions(predictions);
            if (loss.getName().contains("legal_actions")) {
                lossComponents[i] = ((MyBCELoss) loss).evaluatePartA(innerLabels, innerPredictions);
                NDArray  mask = lossComponents[i].stopGradient().lte(0.3f);

                NDArray intArray = mask.toType(DataType.INT32, false);
                 mask = intArray.min(new int[]{1}, true);

                legalActionMasks.add(mask);
                iMap[i]  =  legalActionMasks.size() - 1;
                lossComponents[i] = lossComponents[i].sum(new int[]{1}, true);  // this is done again in evaluatePartB (could be optimized)
            } else if (loss.getName().contains("reward")) {
                lossComponents[i] = ((MyL2Loss) loss).evaluatePartA(innerLabels, innerPredictions);
                NDArray  mask = lossComponents[i].stopGradient().lte(0.01f);
                rewardMasks.add(mask);
                iMap[i]  =  rewardMasks.size() - 1;
            }
//            else if (loss.getName().contains("similarity")) {
//                lossComponents[i] = ((MySimilarityLoss) loss).evaluatePartA(innerLabels, innerPredictions);
//                iMap[i]  =  sCount++;
//            }
            else {
                lossComponents[i] = loss.evaluate(innerLabels, innerPredictions);
            }
        }
   //     List<NDArray> okMasksReward = new ArrayList<>();
    //    List<NDArray> okMasksLegalActions = new ArrayList<>();
     //   List<NDArray> okMasksSimilarity = new ArrayList<>();



        if (legalActionMasks.size() != 1) throw new MuZeroException("legalActionMasks.size() != 1");
        if (rewardMasks.size() != 1) throw new MuZeroException("rewardMasks.size() != 1");

        NDArray okMask = legalActionMasks.get(0).logicalAnd(rewardMasks.get(0));

//        NDManager manager = legalActionMasks.get(0).getManager();
//        NDArray oks = manager.ones(legalActionMasks.get(0).getShape(),  BOOLEAN);
//        okMasksReward.add(oks);
//        okMasksLegalActions.add(oks);
//       // okMasksSimilarity.add(oks);
//        for (int i = 0; i < rewardMasks.size()-1; i++) {
//            okMasksReward.add(okMasksReward.get(i).logicalAnd(rewardMasks.get(i)));
//        }
//        for (int i = 0; i < legalActionMasks.size()-1; i++) {
//            okMasksLegalActions.add(okMasksLegalActions.get(i).logicalAnd(legalActionMasks.get(i)));
//        }
//        for (int i = 0; i < rewardMasks.size()-1; i++) {
//            okMasksSimilarity.add(okMasksReward.get(i).logicalOr(okMasksLegalActions.get(i+1)));
//        }

        for (int i = 0; i < components.size(); i++) {
            Loss loss = ((MyIndexLoss)components.get(i)).getLoss();
            if (loss.getName().contains("legal_actions")) {
           //    NDArray intMask = okMasksLegalActions.get(iMap[i]).toType(DataType.INT32, true).stopGradient();
            //    lossComponents[i] = lossComponents[i].mul(intMask);
                lossComponents[i] = ((MyBCELoss) loss).evaluatePartB(lossComponents[i]);
            } else if (loss.getName().contains("reward")) {
        //        NDArray intMask = okMasksReward.get(iMap[i]).toType(DataType.INT32, true).stopGradient();
        //        lossComponents[i] = lossComponents[i].mul(intMask);
                lossComponents[i] = ((MyL2Loss) loss).evaluatePartB(lossComponents[i]);
            }
//            else if (loss.getName().contains("similarity")) {
//                NDArray intMask = okMasksSimilarity.get(iMap[i]).toType(DataType.INT32, true).stopGradient();
//                lossComponents[i] = lossComponents[i].mul(intMask);
//                lossComponents[i] = ((MySimilarityLoss) loss).evaluatePartB(lossComponents[i]);;
//            }
        }
        return new Pair(NDArrays.add(lossComponents), okMask);
    }

}

