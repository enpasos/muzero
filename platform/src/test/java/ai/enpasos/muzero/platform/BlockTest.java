package ai.enpasos.muzero.platform;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.BroadcastBlock;
import ai.enpasos.mnist.blocks.SqueezeExciteExt;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.cmainfunctions.PredictionBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.cmainfunctions.MainRepresentationOrDynamicsBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel.BottleneckResidualBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel.ResidualTower;
import ai.enpasos.muzero.platform.config.NetworkType;
import ai.enpasos.muzero.platform.config.ValueHeadType;
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
    void broadcastBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./build/BroadcastBlock.onnx",
            BroadcastBlock.builder().setUnits(3 * 3).build(),
            List.of(new Shape(1, 128, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void squeezeExciteRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./build/SqueezeExciteBlock.onnx",
            new SqueezeExciteExt(128, 10),
            List.of(new Shape(1, 128, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }



    @Test
    void bottleneckResidualRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./build/BottleneckResidualBlock.onnx",
            new BottleneckResidualBlock(128, 64),
            List.of(new Shape(1, 128, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void residualTowerRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./build/ResidualTowerBlock.onnx",
            ResidualTower.builder()
                .numResiduals(8)
                .numChannels(128)
                .numBottleneckChannels(64)
                .broadcastEveryN(8)
                .height(3)
                .width(3)
                .build(),
            List.of(new Shape(1, 128, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void broadcastBlockZERO() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./build/BroadcastBlock.onnx",
            BroadcastBlock.builder().setUnits(3 * 3).build(),
            List.of(new Shape(1, 128, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }

    @Test
    void squeezeExciteZERO() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./build/SqueezeExciteBlock.onnx",
            new SqueezeExciteExt(128, 10),
            List.of(new Shape(1, 128, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }



    @Test
    void residualTowerZERO() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./build/ResidualTowerBlock.onnx",
            ResidualTower.builder()
                .numResiduals(8)
                .numChannels(128)
                .numBottleneckChannels(64)
                .broadcastEveryN(8)
                .width(3)
                .height(3)
                .build(),
            List.of(new Shape(1, 128, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }

//    @Test
//    void representationOrDynamicsZERO() throws Exception {
//        boolean check = compareOnnxWithDJL(
//            "./build/RepresentationOrDynamicsBlock.onnx",
//            new MainRepresentationOrDynamicsBlock(NetworkType.CON, 3, 3, 3, 128, 64,8),
//            List.of(new Shape(1, 3, 3, 3)),
//            ZERO);
//        Assertions.assertTrue(check);
//    }

    @Test
    void predictionZERO() throws Exception {

        boolean check = compareOnnxWithDJL(
            "./build/PredictionBlock.onnx",
            new PredictionBlock(NetworkType.CON, 3, 128, true, 9, ValueHeadType.EXPECTED),
            List.of(new Shape(1, 5, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }

//    @Test
//    void representationOrDynamicsRANDOM() throws Exception {
//        boolean check = compareOnnxWithDJL(
//            "./build/RepresentationOrDynamicsBlock.onnx",
//            new MainRepresentationOrDynamicsBlock(NetworkType.CON, 3, 3, 3, 128, 64, 8),
//            List.of(new Shape(1, 3, 3, 3)),
//            RANDOM);
//        Assertions.assertTrue(check);
//    }

    @Test
    void predictionRANDOM() throws Exception {

        boolean check = compareOnnxWithDJL(
            "./build/PredictionBlock.onnx",
            new PredictionBlock(NetworkType.CON, 3, 128, true, 9, ValueHeadType.EXPECTED),
            List.of(new Shape(1, 5, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

}
