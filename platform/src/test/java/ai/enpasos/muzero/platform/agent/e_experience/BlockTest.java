package ai.enpasos.muzero.platform.agent.e_experience;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.BroadcastBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.DynamicsBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.PredictionBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.RepresentationBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.BottleneckResidualBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.ResidualTower;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static ai.enpasos.mnist.blocks.BlockTestHelper.Testdata.RANDOM;
import static ai.enpasos.mnist.blocks.BlockTestHelper.Testdata.ZERO;
import static ai.enpasos.mnist.blocks.BlockTestHelper.compareOnnxWithDJL;


@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
class BlockTest {

    @Autowired
    MuZeroConfig config;


    @Test
    void dynamicsBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/DynamicsBlock.onnx",
                 DynamicsBlock.newDynamicsBlock(config),
                List.of(new Shape(1, 256, 3, 3), new Shape(1, 1, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void representationBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/RepresentationBlock.onnx",
                RepresentationBlock.builder().config(config).build(),
                List.of(new Shape(1, 3, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }


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



    @Test
    void predictionZERO() throws Exception {

        boolean check = compareOnnxWithDJL(
            "./build/PredictionBlock.onnx",
            new PredictionBlock(128, true, 9, true ),
            List.of(new Shape(1, 5, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }



    @Test
    void predictionRANDOM() throws Exception {

        boolean check = compareOnnxWithDJL(
            "./build/PredictionBlock.onnx",
            new PredictionBlock(128, true, 9, true ),
            List.of(new Shape(1, 5, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

}
