package ai.enpasos.muzero.platform.agent.e_experience;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.BroadcastBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.DynamicsBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.PredictionHeads;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.RepresentationBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.RepresentationStart;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.*;
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
    void causalBroadcastResidualBlock2RANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBroadcastResidualBlock2.onnx",
                new CausalBroadcastResidualBlock(3,3, config.getRepresentation().getRules(), false),
                List.of(new Shape(1, 1, 3, 3), new Shape(1, 100, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }


    @Test
    void causalBottleneckResidualLayersBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualLayersBlock.onnx",
                new CausalBottleneckResidualLayersBlock(config.getRepresentation(), 0.75, false),
                List.of(new Shape(1, 100, 3, 3), new Shape(1, 100, 3, 3), new Shape(1, 100, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void causalBottleneckResidualLayersWithRescaleBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualLayersWithRescaleBlock.onnx",
                new CausalBottleneckResidualLayersBlock(config.getRepresentation(), 0.75, true),
                List.of(new Shape(1, 100, 3, 3), new Shape(1, 100, 3, 3), new Shape(1, 100, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void causalBottleneckResidualLayersBlock2RANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualLayersBlock2.onnx",
                new CausalBottleneckResidualLayersBlock(config.getRepresentation(), 0.75, false),
                List.of(new Shape(1, 100, 3, 3), new Shape(1, 100, 3, 3), new Shape(1, 100, 3, 3), new Shape(1, 3, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void representationStartBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/RepresentationStartBlock.onnx",
                new RepresentationStart(config),
                List.of(new Shape(1, 3, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void bottleneckResidualBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/BottleneckResidualBlock.onnx",
                new BottleneckResidualBlock(80, 60),
                List.of(new Shape(1, 80, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void causalBottleneckResidualBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualBlock.onnx",
                new CausalBottleneckResidualBlock(  config.getRepresentation().getRules(), 0.75,false),
                List.of(new Shape(1, 100, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void causalBottleneckResidualBlockWithRescaleRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualBlockWithRescale.onnx",
                new CausalBottleneckResidualBlock(config.getRepresentation().getRules(), 0.75,true),
                List.of(new Shape(1, 100, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void causalBottleneckResidualBlock2RANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualBlock2.onnx",
                new CausalBottleneckResidualBlock(config.getRepresentation().getRules(), 0.75, false),
                List.of(new Shape(1, 1, 3, 3), new Shape(1, 100, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

//    @Test
//    void endingAppenderRANDOM() throws Exception {
//        boolean check = compareOnnxWithDJL(
//                "./build/EndingAppender.onnx",
//                EndingAppender.newEndingAppender(BroadcastBlock.builder().setUnits(3 * 3).build(), 3),
//                List.of(new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3),new Shape(1, 80, 3, 3),new Shape(1, 1, 3, 3)),
//                RANDOM);
//        Assertions.assertTrue(check);
//    }

    @Test
    void dynamicsBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/DynamicsBlock.onnx",
                 DynamicsBlock.newDynamicsBlock(config, config.getGeneration()),
                List.of(new Shape(1, 100, 3, 3), new Shape(1, 100, 3, 3),new Shape(1, 100, 3, 3),new Shape(1, 1, 3, 3)),
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
        PredictionHeads block = new PredictionHeads( true, 9);
        block.setWithPolicy(true);
        block.setWithValue(true);
        block.setWithLegalAction(true);
        block.setWithReward(true);
        boolean check = compareOnnxWithDJL(
            "./build/PredictionBlock.onnx",
            block,
            List.of(new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }



    @Test
    void predictionRANDOM() throws Exception {
        PredictionHeads block = new PredictionHeads( true, 9);
        block.setWithPolicy(true);
        block.setWithValue(true);
        block.setWithLegalAction(true);
        block.setWithReward(true);
        boolean check = compareOnnxWithDJL(
            "./build/PredictionBlock.onnx",
            block,
            List.of(new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void predictionWithRewardRANDOM() throws Exception {
        PredictionHeads predictionHeads = new PredictionHeads( true, 9);
predictionHeads.setWithReward(true);
        predictionHeads.setWithValue(true);
        predictionHeads.setWithPolicy(true);
        predictionHeads.setWithLegalAction(true);
        boolean check = compareOnnxWithDJL(
                "./build/PredictionBlockWithReward.onnx",
                predictionHeads,
                List.of(new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

}
