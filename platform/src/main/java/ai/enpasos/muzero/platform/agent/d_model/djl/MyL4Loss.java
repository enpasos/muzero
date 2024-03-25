package ai.enpasos.muzero.platform.agent.d_model.djl;

/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.training.loss.Loss;

/**
 * Calculates L2Loss between label and prediction, a.k.a. MSE(Mean Square Error).
 *
 * <p>L2 loss is defined by \(L = \frac{1}{2} \sum_i \vert {label}_i - {prediction}_i \vert^2\)
 */
public class MyL4Loss extends Loss {

    public static final float NULL_VALUE = 1234567f;

    private final float weight;

    /**
     * Calculate L2Loss between the label and prediction, a.k.a. MSE(Mean Square Error).
     */
    public MyL4Loss() {
        this("L4Loss");
    }

    /**
     * Calculate L2Loss between the label and prediction, a.k.a. MSE(Mean Square Error).
     *
     * @param name the name of the loss
     */
    public MyL4Loss(String name) {
        this(name, 1.f / 4);
    }

    /**
     * Calculates L2Loss between the label and prediction, a.k.a. MSE(Mean Square Error).
     *
     * @param name   the name of the loss
     * @param weight the weight to apply on loss value, default 1/2
     */
    public MyL4Loss(String name, float weight) {
        super(name);
        this.weight = weight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NDArray evaluate(NDList label, NDList prediction) {
        NDArray pred = prediction.singletonOrThrow();
        NDArray labelReshaped = label.singletonOrThrow().reshape(pred.getShape());
        NDArray mask = labelReshaped.neq(NULL_VALUE);
        NDArray loss = mask.mul(labelReshaped.sub(pred).pow(4).mul(weight));
        return loss.mean();
    }
}