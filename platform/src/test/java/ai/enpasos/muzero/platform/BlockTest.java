package ai.enpasos.muzero.platform;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.MnistBlock;
import ai.enpasos.mnist.blocks.SqueezeExciteExt;
import ai.enpasos.mnist.blocks.ext.RescaleBlockExt;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.cmainfunctions.PredictionBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.cmainfunctions.RepresentationOrDynamicsBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel.Conv1x1LayerNormRelu;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel.Conv3x3LayerNormRelu;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel.ResidualBlockV2;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel.ResidualTower;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.channels.Channels;
import java.util.List;

import static ai.enpasos.mnist.blocks.BlockTestHelper.Testdata.RANDOM;
import static ai.enpasos.mnist.blocks.BlockTestHelper.Testdata.ZERO;
import static ai.enpasos.mnist.blocks.BlockTestHelper.compareOnnxWithDJL;


@Slf4j
class BlockTest {


    @Test
    void squeezeExciteRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./target/SqueezeExciteBlock.onnx",
            new SqueezeExciteExt(128, 10),
            List.of(new Shape(1, 128, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void residualRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./target/ResidualBlockV2.onnx",
            new ResidualBlockV2(128, 10),
            List.of(new Shape(1, 128, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void residualTowerRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./target/ResidualTowerBlock.onnx",
            ResidualTower.builder()
                .numResiduals(3)
                .numChannels(128)
                .squeezeChannelRatio(10)
                .build(),
            List.of(new Shape(1, 128, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void squeezeExciteZERO() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./target/SqueezeExciteBlock.onnx",
            new SqueezeExciteExt(128, 10),
            List.of(new Shape(1, 128, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }

    @Test
    @Disabled
    void residualZERO() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./target/ResidualBlockV2.onnx",
            new ResidualBlockV2(128, 10),
            List.of(new Shape(1, 128, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }

    @Test
    @Disabled
    void residualTowerZERO() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./target/ResidualTowerBlock.onnx",
            ResidualTower.builder()
                .numResiduals(3)
                .numChannels(128)
                .squeezeChannelRatio(10)
                .build(),
            List.of(new Shape(1, 128, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }

    @Test
    @Disabled
    void representationOrDynamicsZERO() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./target/RepresentationOrDynamicsBlock.onnx",
           new RepresentationOrDynamicsBlock(3, 128, 10, 5),
            List.of(new Shape(1, 3, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }

    @Test
    @Disabled
    void predictionZERO() throws Exception {

        boolean check = compareOnnxWithDJL(
            "./target/PredictionBlock.onnx",
            new PredictionBlock(128, true, 9),
            List.of(new Shape(1, 5, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }

    @Test
    void representationOrDynamicsRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
            "./target/RepresentationOrDynamicsBlock.onnx",
            new RepresentationOrDynamicsBlock(3, 128, 10, 5),
            List.of(new Shape(1, 3, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void predictionRANDOM() throws Exception {

        boolean check = compareOnnxWithDJL(
            "./target/PredictionBlock.onnx",
            new PredictionBlock(128, true, 9),
            List.of(new Shape(1, 5, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

}
