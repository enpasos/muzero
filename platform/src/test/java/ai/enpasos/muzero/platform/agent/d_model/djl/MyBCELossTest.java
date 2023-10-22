package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MyBCELossTest {

    @Test
    void evaluateA() {
        MyBCELoss loss = new MyBCELoss("loss_bce_" + 0, 1.0f, 0);

        NDManager manager = NDManager.newBaseManager();
        NDArray label = manager.create(new float[]{0.9f});
        Shape shape = new Shape(1);
        label = label.reshape(shape);

        NDArray prediction = manager.create(new float[]{3f});
        prediction = prediction.reshape(shape);

        assertEquals(-0.04858732968568802, loss.logSigmoid(prediction).getFloat());
        assertEquals(-3.0485873222351074, loss.logOneMinusSigmoid(prediction).getFloat());

        assertEquals(0.34858739376068115, loss.evaluate(new NDList(label), new NDList(prediction)).getFloat());
    }


    @Test
    void evaluateB() {
        MyBCELoss loss = new MyBCELoss("loss_bce_" + 0, 1.0f, 0);

        NDManager manager = NDManager.newBaseManager();
        NDArray label = manager.create(new float[]{0.3f});
        Shape shape = new Shape(1);
        label = label.reshape(shape);

        NDArray prediction = manager.create(new float[]{-2f});
        prediction = prediction.reshape(shape);

        assertEquals(-2.1269280910491943, loss.logSigmoid(prediction).getFloat());
        assertEquals(-0.12692804634571075, loss.logOneMinusSigmoid(prediction).getFloat());

        assertEquals(0.7269281148910522, loss.evaluate(new NDList(label), new NDList(prediction)).getFloat());
    }

    @Test
    void evaluateC() {
        MyBCELoss loss = new MyBCELoss("loss_bce_" + 0, 1.0f, 0);

        NDManager manager = NDManager.newBaseManager();
        NDArray label = manager.create(new float[]{0.9f, 0.3f});
        Shape shape = new Shape(2);
        label = label.reshape(shape);

        NDArray prediction = manager.create(new float[]{3f, -2f});
        prediction = prediction.reshape(shape);

        assertEquals(1.0755155086517334, loss.evaluate(new NDList(label), new NDList(prediction)).getFloat());
    }
    @Test
    void evaluateD() {
        MyBCELoss loss = new MyBCELoss("loss_bce_" + 0, 1.0f, 1);

        NDManager manager = NDManager.newBaseManager();
        NDArray label = manager.create(new float[]{0.9f, 0.3f, 0.9f, 0.3f});
        Shape shape = new Shape(2, 2);
        label = label.reshape(shape);

        NDArray prediction = manager.create(new float[]{3f, -2f, 3f, -2f});
        prediction = prediction.reshape(shape);

        assertEquals(1.0755155086517334,  loss.evaluate(new NDList(label), new NDList(prediction)).getFloat());
    }

}
