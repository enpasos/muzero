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

package ai.enpasos.muzero.platform.agent.d_model.djl;


import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.training.loss.Loss;
import org.jetbrains.annotations.NotNull;

/**
 * {@code BCELoss} is a type of {@link Loss} that calculates the binary cross entropy loss.
 *
 * <p>{@code label} should contain probability distribution and its shape should be the same as
 * the shape of {@code prediction}.
 */
public class MyBCELoss extends Loss {

    private final float weight;
    private final int classAxis;


    /**
     * Creates a new instance of {@code MyBCELoss} with default parameters.
     */
    public MyBCELoss() {
        this("BCELoss");
    }

    /**
     * Creates a new instance of {@code MyBCELoss} with default parameters.
     *
     * @param name the name of the loss
     */
    public MyBCELoss(String name) {
        this(name, 1, -1 );
    }

    /**
     * Creates a new instance of {@code MyBCELoss} with the given parameters.
     *
     * @param name        the name of the loss
     * @param weight      the weight to apply on the loss value, default 1
     * @param classAxis   the axis that represents the class probabilities, default -1
     */
    public MyBCELoss(
        String name, float weight, int classAxis ) {
        super(name);
        this.weight = weight;
        this.classAxis = classAxis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NDArray evaluate(@NotNull NDList label, @NotNull NDList prediction) {
        NDArray pred = prediction.singletonOrThrow();
        NDArray lab = label.singletonOrThrow();

        NDArray loss;


        lab = lab.reshape(pred.getShape());


        NDArray lossA = logSigmoid(pred).mul(lab).neg();
        NDArray lossB = logOneMinusSigmoid(pred).mul(lab.mul(-1).add(1)).neg();

        loss = lossA.add(lossB).sum(new int[]{classAxis}, true);
    //    loss = lossA.sum(new int[]{classAxis}, true);

        if (weight != 1) {
            loss = loss.mul(weight);
        }
        // return loss.mean();
        return loss;
    }





    //   \min (0,x) - \ln( 1+e^{-|x|})
    // naive implementation: sigmoid (x).log();
    // sigmoid:   x.mul(-1).exp().add(1).getNDArrayInternal().rdiv(1);
    public NDArray logSigmoid(NDArray x) {
        return x.abs().neg().exp().add(1).log().neg().add(x.minimum(0));
    }

//    public NDArray sigmoid (NDArray x) {
//        return x.mul(-1).exp().add(1).getNDArrayInternal().rdiv(1);
//    }


    public NDArray logOneMinusSigmoid(NDArray x) {
        return x.abs().neg().exp().add(1).log().add(x.maximum(0)).neg();
    }


}
