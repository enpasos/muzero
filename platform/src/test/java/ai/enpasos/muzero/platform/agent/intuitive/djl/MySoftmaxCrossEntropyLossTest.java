package ai.enpasos.muzero.platform.agent.intuitive.djl;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MySoftmaxCrossEntropyLossTest {

    @Test
    void evaluateNormal() {
        MySoftmaxCrossEntropyLoss loss = new MySoftmaxCrossEntropyLoss("loss_policy_" + 0, 1.0f, 1, false, true);

        NDManager manager = NDManager.newBaseManager();

        Shape shape = new Shape(2, 3);

        NDArray label = manager.create(new float[]{0.8f, 0f, 0.1f, 0, 1f, 0});
        label = label.reshape(shape);

        NDArray prediction = manager.create(new float[]{0.8f, 0f, 0.1f, 0, 1f, 0});
        prediction = prediction.reshape(shape);


        assertEquals(0.61030173f, loss.evaluate(new NDList(label), new NDList(prediction)).getFloat());
    }

    @Test
    void evaluateLegalActions1() {
        MySoftmaxCrossEntropyLoss loss = new MySoftmaxCrossEntropyLoss("loss_policy_" + 0, 1.0f, 1, false, true);
loss.setUseLabelAsLegalCategoriesFilter(true);
        NDManager manager = NDManager.newBaseManager();

        Shape shape = new Shape(1, 2);


        NDArray label = manager.create(new float[]{1f, 0f});
        label = label.reshape(shape);

        NDArray prediction = manager.create(new float[]{0.8f, 0.2f});
        prediction = prediction.reshape(shape);


        assertEquals(0.437488f, loss.evaluate(new NDList(label), new NDList(prediction)).getFloat());
    }

    @Test
    void evaluateLegalActions() {
        MySoftmaxCrossEntropyLoss loss = new MySoftmaxCrossEntropyLoss("loss_policy_" + 0, 1.0f, 1, false, true );
        loss.setUseLabelAsLegalCategoriesFilter(true);
        NDManager manager = NDManager.newBaseManager();

        Shape shape = new Shape(2, 3);


        NDArray label = manager.create(new float[]{1f, 0f, 1f, 0, 1f, 0});
        label = label.reshape(shape);

        NDArray prediction = manager.create(new float[]{0.8f, 0f, 0.1f, 0, 1f, 0});
        prediction = prediction.reshape(shape);


        assertEquals(0.7247226f, loss.evaluate(new NDList(label), new NDList(prediction)).getFloat());
    }

    @Test
    void evaluateLegalActions2() {
        MySoftmaxCrossEntropyLoss loss = new MySoftmaxCrossEntropyLoss("loss_policy_" + 0, 1.0f, 1, false, true );
        loss.setUseLabelAsLegalCategoriesFilter(true);
        NDManager manager = NDManager.newBaseManager();

        Shape shape = new Shape(2, 3);


        NDArray label = manager.create(new float[]{1f, 0f, 0f, 0, 1f, 0});
        label = label.reshape(shape);

        NDArray prediction = manager.create(new float[]{0.8f, 0f, 0.1f, 0, 1f, 0});
        prediction = prediction.reshape(shape);


        assertEquals(0.608588321f, loss.evaluate(new NDList(label), new NDList(prediction)).getFloat());
    }
}
