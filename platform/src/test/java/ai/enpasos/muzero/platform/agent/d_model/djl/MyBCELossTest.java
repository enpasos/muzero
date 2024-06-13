package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.platform.common.Functions.f2d;
import static org.junit.jupiter.api.Assertions.*;

class MyBCELossTest {

    @Test
    void logSigmoid() {
        NDManager ndManager = NDManager.newBaseManager();
        float[] data = new float[]{0.0f, 0.8f, -0.8f};
        NDArray x = ndManager.create(data);
        NDArray y = MyBCELoss.logSigmoid(x);
        Number[] yN = y.toArray();
        assertEquals(yN[0].doubleValue(), MyBCELoss.logSigmoid(0.0), 0.000001);
        assertEquals(yN[1].doubleValue(), MyBCELoss.logSigmoid(0.8), 0.000001);
        assertEquals(yN[2].doubleValue(), MyBCELoss.logSigmoid(-0.8), 0.000001);
    }

    @Test
    void lossPerItem() {
        NDManager ndManager = NDManager.newBaseManager();
        float[] pred = new float[]{0.0f, 0.8f, -0.8f};
        float[] label = new float[]{1f, 1f, 0f};
        NDArray predND = ndManager.create(pred);
        NDArray labelND = ndManager.create(label);
        MyBCELoss loss = new MyBCELoss();
        NDArray y = loss.evaluatePartA(new NDList(labelND), new NDList(predND));
        Number[] yN = y.toArray();
        assertEquals(yN[0].doubleValue(), MyBCELoss.lossPerItemLogit(label[0], pred[0]), 0.000001);
        assertEquals(yN[1].doubleValue(), MyBCELoss.lossPerItemLogit(label[1], pred[1]), 0.000001);
        assertEquals(yN[2].doubleValue(), MyBCELoss.lossPerItemLogit(label[2], pred[2]), 0.000001);
    }

    @Test
    void okTest() {
        NDManager ndManager = NDManager.newBaseManager();
        float[] pred = new float[]{0.0f, 0.8f, -8f};
        float[] label = new float[]{0f, 0f, 1f};
        NDArray predND = ndManager.create(pred);
        NDArray labelND = ndManager.create(label);
        MyBCELoss loss = new MyBCELoss("MyBCELoss", 1f, 1, 0.03);
        NDArray result = loss.evaluatePartA(new NDList(labelND), new NDList(predND));
        NDArray mask = result.lte(loss.threshold);
        NDArray intArray = mask.toType(DataType.INT32, false);

        assertEquals(loss.isOkLogit(label[0], pred[0]) ? 1f : 0f, intArray.toIntArray()[0] );
        assertEquals(loss.isOkLogit(label[1], pred[1]) ? 1f : 0f, intArray.toIntArray()[1]);
        assertEquals(loss.isOkLogit(label[2], pred[2]) ? 1f : 0f, intArray.toIntArray()[2]);

        assertEquals(loss.isOkLogit(f2d(label) , f2d(pred) ) ? 1f : 0f, intArray.min(new int[]{0}, true).toIntArray()[0]);

    }
}
