package ai.enpasos.muzero.platform.agent.intuitive.djl;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MySimilarityLossTest {

    @Test
    void evaluate() {

        MySimilarityLoss loss = new MySimilarityLoss();

        NDManager manager = NDManager.newBaseManager();

        Shape shape = new Shape(2, 3);

        NDArray label = manager.create(new float[]{1f, 0, 0, 0, 1f, 0});
        label = label.reshape(shape);

        NDArray prediction = manager.create(new float[]{1f, 0, 0, 0, 1f, 0});
        prediction = prediction.reshape(shape);


        assertEquals(0f, loss.evaluate(new NDList(label), new NDList(prediction)).getFloat());

    }
}
