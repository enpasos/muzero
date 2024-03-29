package ai.enpasos.mnist;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.MnistBlock;
import ai.enpasos.mnist.blocks.ext.LayerNormExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.enpasos.mnist.blocks.BlockTestHelper.Testdata.RANDOM;
import static ai.enpasos.mnist.blocks.BlockTestHelper.Testdata.ZERO;
import static ai.enpasos.mnist.blocks.BlockTestHelper.compareOnnxWithDJL;


@Slf4j
class BlockTest {

    @Test
    void layerNorm2ZERO() throws Exception {
        boolean check =
            compareOnnxWithDJL(
                "./build/LayerNorm2.onnx",
                LayerNormExt.builder().build(),
                List.of(new Shape(1, 128, 3, 3)),
                ZERO
            );
        Assertions.assertTrue(check);
    }

    void layerNorm2ZERO2() throws Exception {
        boolean check =
            compareOnnxWithDJL(
                "./build/LayerNorm2.onnx",
                LayerNormExt.builder().build(),
                List.of(new Shape(1, 500)),
                ZERO
            );
        Assertions.assertTrue(check);
    }


    @Test
    void layerNorm2RANDOM() throws Exception {
        boolean check =
            compareOnnxWithDJL(
                "./build/LayerNorm2.onnx",
                LayerNormExt.builder().build(),
                List.of(new Shape(1, 128, 3, 3)),
                RANDOM
            );
        Assertions.assertTrue(check);
    }
    @Test
    void layerNorm2RANDOM2() throws Exception {
        boolean check =
            compareOnnxWithDJL(
                "./build/LayerNorm2.onnx",
                LayerNormExt.builder().build(),
                List.of(new Shape(1, 500)),
                RANDOM
            );
        Assertions.assertTrue(check);
    }
    @Test
    void layerNormZERO() throws Exception {
        boolean check =
            compareOnnxWithDJL(
                "./build/LayerNorm.onnx",
                LayerNormExt.builder().build(),
                List.of(new Shape(1, 128, 3, 3)),
                ZERO
            );
        Assertions.assertTrue(check);
    }


    @Test
    void layerNormRANDOM() throws Exception {
        boolean check =
            compareOnnxWithDJL(
                "./build/LayerNorm.onnx",
                LayerNormExt.builder().build(),
                List.of(new Shape(1, 128, 3, 3)),
                RANDOM
            );
        Assertions.assertTrue(check);
    }



    @Test
    void mnistBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./build/MnistBlock.onnx",
            MnistBlock.newMnistBlock(),
            List.of(new Shape(1, 1, 28, 28)),
            RANDOM);
        Assertions.assertTrue(check);
    }




    @Test
    void mnistBlockZERO() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./build/MnistBlock.onnx",
            MnistBlock.newMnistBlock(),
            List.of(new Shape(1, 1, 28, 28)),
            ZERO);
        Assertions.assertTrue(check);
    }

}
