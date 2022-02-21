package ai.enpasos.mnist;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.MnistBlock;
import ai.enpasos.mnist.blocks.SqueezeExciteExt;
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
    void layerNormZERO() throws Exception {
        boolean check =
            compareOnnxWithDJL(
                "./target/LayerNorm.onnx",
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
                "./target/LayerNorm.onnx",
                LayerNormExt.builder().build(),
                List.of(new Shape(1, 128, 3, 3)),
                RANDOM
            );
        Assertions.assertTrue(check);
    }


    @Test
    void squeezeExciteRANDOM() throws Exception {
        boolean check =
            compareOnnxWithDJL(
                "./target/SqueezeExciteBlock.onnx",
                new SqueezeExciteExt(128, 10),
                List.of(new Shape(1, 128, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void mnistBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./target/MnistBlock.onnx",
            MnistBlock.newMnistBlock(),
            List.of(new Shape(1, 1, 28, 28)),
            RANDOM);
        Assertions.assertTrue(check);
    }


    @Test
    void squeezeExciteZERO() throws Exception {
        boolean check =
            compareOnnxWithDJL(
                "./target/SqueezeExciteBlock.onnx",
                new SqueezeExciteExt(128, 10),
                List.of(new Shape(1, 128, 3, 3)),
                ZERO);
        Assertions.assertTrue(check);
    }

    @Test
    void mnistBlockZERO() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./target/MnistBlock.onnx",
            MnistBlock.newMnistBlock(),
            List.of(new Shape(1, 1, 28, 28)),
            ZERO);
        Assertions.assertTrue(check);
    }

}
