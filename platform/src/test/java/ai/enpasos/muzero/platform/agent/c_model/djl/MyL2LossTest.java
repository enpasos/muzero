package ai.enpasos.muzero.platform.agent.c_model.djl;

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MyL2LossTest {

    @Test
    void evaluate() {
        MyL2Loss loss = new MyL2Loss();
        NDList label = new NDList();
        NDList prediction = new NDList();
        NDManager manager = NDManager.newBaseManager();
        label.add(manager.create(new float[]{1, 2, 3, MyL2Loss.NULL_VALUE}));
        prediction.add(manager.create(new float[]{2, 3, 4, 19})); // no force on the last element


        assertEquals(0.375f, loss.evaluate(label, prediction).getFloat());
    }

}
