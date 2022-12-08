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

package ai.enpasos.muzero.platform.agent.intuitive.djl;


import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.training.loss.Loss;
import ai.enpasos.muzero.platform.config.TrainingTypeKey;
import org.jetbrains.annotations.NotNull;

/**
 * {@code SoftmaxCrossEntropyLoss} is a type of {@link Loss} that calculates the softmax cross
 * entropy loss.
 *
 * <p>If {@code sparse_label} is {@code true} (default), {@code label} should contain integer
 * category indicators. Then, \(L = -\sum_i \log p_{i, label_i}\). If {@code sparse_label} is {@code
 * false}, {@code label} should contain probability distribution and its shape should be the same as
 * the shape of {@code prediction}. Then, \(L = -\sum_i \sum_j {label}_j \log p_{ij}\).
 */
public class MySoftmaxCrossEntropyLoss extends Loss {

    private final float weight;
    private final int classAxis;
    private final boolean sparseLabel;
    private final boolean fromLogit;

   private  boolean  useLabelAsLegalCategoriesFilter;

    /**
     * Creates a new instance of {@code SoftmaxCrossEntropyLoss} with default parameters.
     */
    public MySoftmaxCrossEntropyLoss() {
        this("SoftmaxCrossEntropyLoss");
    }

    /**
     * Creates a new instance of {@code SoftmaxCrossEntropyLoss} with default parameters.
     *
     * @param name the name of the loss
     */
    public MySoftmaxCrossEntropyLoss(String name) {
        this(name, 1, -1, true, false);
    }

    /**
     * Creates a new instance of {@code SoftmaxCrossEntropyLoss} with the given parameters.
     *
     * @param name        the name of the loss
     * @param weight      the weight to apply on the loss value, default 1
     * @param classAxis   the axis that represents the class probabilities, default -1
     * @param sparseLabel whether labels are integer array or probabilities, default true
     * @param fromLogit   whether predictions are log probabilities or un-normalized numbers, default
     *                    false
     */
    public MySoftmaxCrossEntropyLoss(
        String name, float weight, int classAxis, boolean sparseLabel, boolean fromLogit ) {
        super(name);
        this.weight = weight;
        this.classAxis = classAxis;
        this.sparseLabel = sparseLabel;
        this.fromLogit = fromLogit;
    //    this.useLabelAsLegalCategoriesFilter =  useLabelAsLegalCategoriesFilter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NDArray evaluate(@NotNull NDList label, @NotNull NDList prediction) {
        NDArray pred = prediction.singletonOrThrow();
        NDArray lab = label.singletonOrThrow();

        if (fromLogit) {
            if (useLabelAsLegalCategoriesFilter) {
              // normalize needs djl = "0.20.0" :-(  not used for now because of performance
              //  lab = pred.softmax(classAxis).mul(lab).normalize(1, classAxis, 1e-12);
                // therefore for now explicitly

                lab = pred.stopGradient().mul(lab);
                lab = lab.eq(0).mul(-1e12f).add(lab).softmax(classAxis);
//
//                ..softmax(classAxis);
//                NDArray sum = lab.sum(new int[] {1}, true).add(1e-12);
//                lab = lab.div(sum);
            }
            pred = pred.logSoftmax(classAxis);

        }
        NDArray loss;

        if (sparseLabel) {
            NDIndex pickIndex =
                new NDIndex()
                    .addAllDim(Math.floorMod(classAxis, pred.getShape().dimension()))
                    .addPickDim(lab);
            loss = pred.get(pickIndex).neg();
        } else {
            lab = lab.reshape(pred.getShape());
            loss = pred.mul(lab).neg().sum(new int[]{classAxis}, true);
        }
        if (weight != 1) {
            loss = loss.mul(weight);
        }
        return loss.mean();
    }

    public boolean isUseLabelAsLegalCategoriesFilter() {
        return useLabelAsLegalCategoriesFilter;
    }

    public void setUseLabelAsLegalCategoriesFilter(boolean useLabelAsLegalCategoriesFilter) {
       this.useLabelAsLegalCategoriesFilter = useLabelAsLegalCategoriesFilter;
    }
}
