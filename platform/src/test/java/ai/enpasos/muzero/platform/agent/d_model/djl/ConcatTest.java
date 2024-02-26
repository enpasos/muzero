package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcatTest {

    @Test
    void evaluate() {
        NDManager manager = NDManager.newBaseManager();

        NDArray x = manager.create(new float[] {1f, 2f, 3f, 4f}, new Shape(2, 2));


        NDArray y = NDArrays.concat(new NDList(x,x,x), 1);
        assertArrayEquals(new long[] {2,6}, y.getShape().getShape());

        y = NDArrays.concat(new NDList(x,x,x), 0);
        assertArrayEquals(new long[] {6,2}, y.getShape().getShape());
    }

}
