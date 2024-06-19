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




    public NDArray  evaluateWhatToTrain(NDList labels, NDList predictions, boolean[][][] bOK, int[] from, Statistics statistics) {
        int symmetryEnhancementFactor = (int)labels.get(0).getShape().get(0)/bOK.length;  // TODO



        SomeSerialization.saveNDList(labels, "labels.dat");
        SomeSerialization.saveNDList(predictions, "predictions.dat");

        SomeSerialization.saveBooleanArray(bOK, "bOK.dat");
        SomeSerialization.saveIntArray(from, "from.dat");


        NDArray[] lossComponents = new NDArray[components.size()];
        int[] iMap = new int[components.size()];
        List<NDArray> rewardMasks = new ArrayList<>();
        List<NDArray> legalActionMasks = new ArrayList<>();
        int sCount = 0;
        for (int i = 0; i < components.size(); i++) {
            Loss loss = ((MyIndexLoss)components.get(i)).getLoss();
            NDList innerLabels = ((MyIndexLoss)components.get(i)).getLabels(labels);
            NDList innerPredictions = ((MyIndexLoss)components.get(i)).getPredictions(predictions);
            if (loss.getName().contains("legal_actions")) {
                lossComponents[i] = ((MyBCELoss) loss).evaluatePartA(innerLabels, innerPredictions);
                NDArray  mask = lossComponents[i].stopGradient().lte(this.legalActionLossMaxThreshold);
                NDArray intArray = mask.toType(DataType.INT32, false);
                 mask = intArray.min(new int[]{1}, true);
                legalActionMasks.add(mask.toType(DataType.BOOLEAN, false));
                iMap[i]  =  legalActionMasks.size() - 1;
                lossComponents[i] = lossComponents[i].sum(new int[]{1}, true);  // this is done again in evaluatePartB (could be optimized)
            } else if (loss.getName().contains("reward")) {
                lossComponents[i] = ((MyL2Loss) loss).evaluatePartA(innerLabels, innerPredictions);
                NDArray  mask = lossComponents[i].stopGradient().lte(this.rewardLossThreshold);
                rewardMasks.add(mask);
                iMap[i]  =  rewardMasks.size() - 1;
            }
            else {
                lossComponents[i] = loss.evaluate(innerLabels, innerPredictions);
            }
        }


         for (int tau = 0; tau < legalActionMasks.size(); tau++) {

                 NDArray okMask = ( rewardMasks.size() > tau && tau > 0) ?
                            legalActionMasks.get(tau).logicalAnd(rewardMasks.get(tau - 1))
                         :
                            legalActionMasks.get(tau);


             // update BOK
            boolean[] okUpdateInfo = okMask.toBooleanArray();
            for (int j = 0; j < bOK.length; j++) {
                int n = bOK[j].length;
                int tFrom =  from[j] ;
                int tTo = tFrom + tau;
                if (tTo >= n) continue;
                bOK[j][tFrom][tTo] = okUpdateInfo[j];   // not taking the symmetry into account here, a voting mechanism could be implemented
            }
         }
        float[][][] trainingNeeded = ZipperFunctions.trainingNeededFloat(bOK, 1f, true);

        List<NDArray> masks = new ArrayList<>();
        NDManager ndManager = legalActionMasks.get(0).getManager();
        for (int tau = 0; tau < legalActionMasks.size(); tau++) {
            float[] trainingNeeded_ = new float[bOK.length * symmetryEnhancementFactor];
            for (int j = 0; j < trainingNeeded.length; j++) {
                int n = bOK[j].length;
                int tFrom =  from[j]  ;
                int tTo = tFrom + tau;
                if (tTo >= n) continue;
                for (int s = 0; s < symmetryEnhancementFactor; s++) {
                    trainingNeeded_[j * symmetryEnhancementFactor + s] = trainingNeeded[j][tFrom][tTo];
                }
            }
            NDArray floatMask = ndManager.create(trainingNeeded_);
            masks.add(floatMask);

        }

        // now the actual masking should happen according to the trainingNeeded
        for (int i = 0; i < components.size(); i++) {
            Loss loss = ((MyIndexLoss) components.get(i)).getLoss();
            if (loss.getName().contains("legal_actions")) {
                NDArray intMask = masks.get(iMap[i]) ;
                int c = (int)intMask.sum().toFloatArray()[0];  // TODO check
                statistics.setCount(statistics.getCount() + c);
                lossComponents[i] = lossComponents[i].mul(intMask);
                lossComponents[i] = ((MyBCELoss) loss).evaluatePartB(lossComponents[i]);
                float v = lossComponents[i].toFloatArray()[0];
                statistics.setSumLossLegalActions(statistics.getSumLossLegalActions() + v);
            } else if (loss.getName().contains("reward")) {
                NDArray intMask = masks.get(iMap[i]) ;
                lossComponents[i] = lossComponents[i].mul(intMask);
                lossComponents[i] = ((MyL2Loss) loss).evaluatePartB(lossComponents[i]);

                float v = lossComponents[i].sum().toFloatArray()[0];
                statistics.setSumLossReward(statistics.getSumLossReward() + v);
            }
        }

        if (lossComponents.length == 1) {
            return lossComponents[0];
        } else {
            return NDArrays.add(lossComponents);
        }
    }

}

