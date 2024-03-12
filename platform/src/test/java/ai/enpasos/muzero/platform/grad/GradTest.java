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

package ai.enpasos.muzero.platform.grad;

import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.GradientCollector;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyL2Loss;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


class GradTest {

    @Test
    void test1() {


        NDManager manager = NDManager.newBaseManager();


        NDArray X = manager.create(new float[]{1});
        NDArray y = manager.create(new float[]{7});


        NDArray w = manager.create(new float[]{2});
        NDArray b = manager.create(new float[]{3});
        NDList params = new NDList(w, b);


        // Attach Gradients
        for (NDArray param : params) {
            param.setRequiresGradient(true);
        }


        try (GradientCollector gc = Engine.getInstance().newGradientCollector()) {
            // Minibatch loss in X and y
            NDArray l = squaredLoss(linreg(X, params.get(0), params.get(1)), y);
            gc.backward(l);  // Compute gradient on l with respect to w and b
        }

        float wGrad = w.getGradient().toFloatArray()[0];
        float bGrad = b.getGradient().toFloatArray()[0];

        System.out.println("wGrad:" + wGrad);
        System.out.println("bGrad:" + bGrad);


        assertEquals(-2f, wGrad);
        assertEquals(-2f, bGrad);
    }


    @Test
    void test2() {
        NDManager manager = NDManager.newBaseManager();

        NDArray X = manager.create(new float[]{1});
        NDArray y = manager.create(new float[]{7});

        NDArray w = manager.create(new float[]{2});
        NDArray b = manager.create(new float[]{3});
        NDList params = new NDList(w, b);


        // Attach Gradients
        for (NDArray param : params) {
            param.setRequiresGradient(true);
        }


        try (GradientCollector gc = Engine.getInstance().newGradientCollector()) {
            // Minibatch loss in X and y
            NDArray l = squaredLoss(linreg(linreg(X, params.get(0), params.get(1)), params.get(0), params.get(1)), y);
            gc.backward(l);  // Compute gradient on l with respect to w and b
        }

        float wGrad = w.getGradient().toFloatArray()[0];
        float bGrad = b.getGradient().toFloatArray()[0];

        System.out.println("wGrad:" + wGrad);
        System.out.println("bGrad:" + bGrad);


        assertEquals(42f, wGrad);
        assertEquals(18f, bGrad);
    }


    @Test
    void test3() {
        NDManager manager = NDManager.newBaseManager();

        NDArray X = manager.create(new float[]{1});
        NDArray y = manager.create(new float[]{7});

        NDArray w = manager.create(new float[]{2});
        NDArray b = manager.create(new float[]{3});
        NDList params = new NDList(w, b);


        // Attach Gradients
        for (NDArray param : params) {
            param.setRequiresGradient(true);
        }


        try (GradientCollector gc = Engine.getInstance().newGradientCollector()) {
            // Minibatch loss in X and y
            NDArray l = squaredLoss(linreg(linreg(X, params.get(0), params.get(1)).scaleGradient(0.5), params.get(0), params.get(1)), y);
            gc.backward(l);  // Compute gradient on l with respect to w and b
        }

        float wGrad = w.getGradient().toFloatArray()[0];
        float bGrad = b.getGradient().toFloatArray()[0];

        System.out.println("wGrad:" + wGrad);
        System.out.println("bGrad:" + bGrad);


        assertEquals(36f, wGrad);
        assertEquals(12f, bGrad);
    }


    public NDArray squaredLoss(NDArray yHat, NDArray y) {
        return (yHat.sub(y.reshape(yHat.getShape()))).mul
                ((yHat.sub(y.reshape(yHat.getShape())))).div(2);
    }


    public NDArray linreg(NDArray X, NDArray w, NDArray b) {
        return X.dot(w).add(b);
    }


}
