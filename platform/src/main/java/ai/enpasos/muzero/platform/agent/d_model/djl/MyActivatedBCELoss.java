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
import ai.djl.training.loss.Loss;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * {@code BCELoss} is a type of {@link Loss} that calculates the binary cross entropy loss.
 *
 * <p>{@code label} should contain probability distribution and its shape should be the same as
 * the shape of {@code prediction}.
 */
public class MyActivatedBCELoss extends Loss {

    private final float weight;
    private final int classAxis;
    private final float activationThreshold;


    /**
     * Creates a new instance of {@code MyBCELoss} with default parameters.
     */
    public MyActivatedBCELoss() {
        this("BCELoss");
    }

    /**
     * Creates a new instance of {@code MyBCELoss} with default parameters.
     *
     * @param name the name of the loss
     */
    public MyActivatedBCELoss(String name) {
        this(name, 1, -1, 0.03f );
    }

    /**
     * Creates a new instance of {@code MyBCELoss} with the given parameters.
     *
     * @param name        the name of the loss
     * @param weight      the weight to apply on the loss value, default 1
     * @param classAxis   the axis that represents the class probabilities, default -1
     */
    public MyActivatedBCELoss(
        String name, float weight, int classAxis, float activationThreshold ) {
        super(name);
        this.weight = weight;
        this.classAxis = classAxis;
        this.activationThreshold = activationThreshold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NDArray evaluate(@NotNull NDList label, @NotNull NDList prediction) {
        NDArray pred = prediction.singletonOrThrow();
        NDArray lab = label.singletonOrThrow();




        lab = lab.reshape(pred.getShape());


        NDArray lossA = logSigmoid(pred).mul(lab).neg();
        NDArray lossB = logOneMinusSigmoid(pred).mul(lab.mul(-1).add(1)).neg();

        NDArray loss = lossA.add(lossB);

        NDArray activation = loss.div(loss.add(activationThreshold));
        NDArray activatedLoss = loss.mul(activation);

        activatedLoss = activatedLoss.sum(new int[]{classAxis}, true);


        if (weight != 1) {
            activatedLoss = activatedLoss.mul(weight);
        }
        return activatedLoss.mean();
    }





    //   \min (0,x) - \ln( 1+e^{-|x|})
    // naive implementation: sigmoid (x).log();
    // sigmoid:   x.mul(-1).exp().add(1).getNDArrayInternal().rdiv(1);
    public static NDArray logSigmoid(NDArray x) {
        return x.abs().neg().exp().add(1).log().neg().add(x.minimum(0));
    }

    public static NDArray sigmoid (NDArray x) {
        return x.mul(-1).exp().add(1).getNDArrayInternal().rdiv(1);
    }
    public static double sigmoid (double x) {
        return 1d/(1d+Math.exp(-x));
    }


    public static NDArray logOneMinusSigmoid(NDArray x) {
        return x.abs().neg().exp().add(1).log().add(x.maximum(0)).neg();
    }


    public static double[] sigmoid(double[] legalActions) {
       return Arrays.stream(legalActions).map(MyActivatedBCELoss::sigmoid).toArray();
    }

    public static double entropy(double[] legalActions) {
        double entropy = 0d;
        for (int i = 0; i < legalActions.length; i++) {
            double p = legalActions[i];
            double[] ps = {p, 1d-p};
            entropy += ai.enpasos.muzero.platform.common.Functions.entropy(ps);
        }
        return entropy;
    }



}
