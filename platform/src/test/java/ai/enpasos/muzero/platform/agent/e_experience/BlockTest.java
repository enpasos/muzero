package ai.enpasos.muzero.platform.agent.e_experience;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.BroadcastBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.InitialRulesBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.*;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.*;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
                new CausalBroadcastResidualBlock(3,3, 80,  20,false),
                List.of(new Shape(1, 1, 3, 3), new Shape(1, 80, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }


    @Test
    void causalBottleneckResidualLayersBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualLayersBlock.onnx",
                new CausalBottleneckResidualLayersBlock(new int[]{80, 80,80}, new int[]{20, 20, 20}, false),
                List.of(new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void causalBottleneckResidualLayersWithRescaleBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualLayersWithRescaleBlock.onnx",
                new CausalBottleneckResidualLayersBlock(new int[]{80, 80,80}, new int[]{20, 20, 20}, true),
                List.of(new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void causalBottleneckResidualLayersBlock2RANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualLayersBlock2.onnx",
                new CausalBottleneckResidualLayersBlock(new int[]{80, 80,80}, new int[]{20, 20, 20}, false),
                List.of(new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3), new Shape(1, 3, 3, 3)),
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
                new CausalBottleneckResidualBlock(80, 60,  20,false),
                List.of(new Shape(1, 80, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void causalBottleneckResidualBlockWithRescaleRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualBlockWithRescale.onnx",
                new CausalBottleneckResidualBlock(80, 60,  20,true),
                List.of(new Shape(1, 80, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void causalBottleneckResidualBlock2RANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/CausalBottleneckResidualBlock2.onnx",
                new CausalBottleneckResidualBlock(80, 60, 20, false),
                List.of(new Shape(1, 1, 3, 3), new Shape(1, 80, 3, 3)),
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
    void dynamicsStartBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/DynamicsStartBlock.onnx",
                new DynamicsStart(config),
                List.of(new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3),new Shape(1, 80, 3, 3),new Shape(1, 1, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void dynamicsBlockRANDOM() throws Exception {
        boolean check = compareOnnxWithDJL(
                "./build/DynamicsBlock.onnx",
                 DynamicsBlock.newDynamicsBlock(config),
                List.of(new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3),new Shape(1, 80, 3, 3),new Shape(1, 1, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }

    @Test
    void representationBlockRANDOM() throws Exception {
        RepresentationBlock representationBlock = RepresentationBlock.builder().config(config).build();
        boolean check = compareOnnxWithDJL(
                "./build/RepresentationBlock.onnx",
                representationBlock,
                List.of(new Shape(1, 3, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }
    @Test
    void representationStartBlockForInitialRulesOnlyRANDOM() throws Exception {
        RepresentationStart representationStart = new RepresentationStart(config);

        boolean check = compareOnnxWithDJL(
                "./build/RepresentationStartBlockForInitialRulesOnly.onnx",
                representationStart.getBlockForInitialRulesOnly(),
                List.of(new Shape(1, 3, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }
    @Test
    void representationBlockForInitialRulesOnlyRANDOM() throws Exception {
        RepresentationBlock representationBlock = RepresentationBlock.builder().config(config).build();
        boolean check = compareOnnxWithDJL(
                "./build/RepresentationBlockForInitialRulesOnly.onnx",
                representationBlock.getBlockForInitialRulesOnly(config),
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
            new PredictionBlock( true, 9 ),
            List.of(new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3)),
            ZERO);
        Assertions.assertTrue(check);
    }


//    @Test
//    void toPredictionRANDOM() throws Exception {
//
//        boolean check = compareOnnxWithDJL(
//                "./build/CausalLayersToPrediction.onnx",
//                new CausalLayersToPrediction( ),
//                List.of(new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3)),
//                RANDOM);
//        Assertions.assertTrue(check);
//    }

    @Test
    void predictionRANDOM() throws Exception {

        boolean check = compareOnnxWithDJL(
            "./build/PredictionBlock.onnx",
            new PredictionBlock( true, 9),
            List.of(new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3)),
            RANDOM);
        Assertions.assertTrue(check);
    }



    @Test
    void predictionWithRewardRANDOM() throws Exception {
        PredictionBlock predictionBlock = new PredictionBlock(  true, 9);
        predictionBlock.setHeadUsage(new boolean[] {true, true, true, true});
        boolean check = compareOnnxWithDJL(
                "./build/PredictionBlockWithReward.onnx",
                predictionBlock,
                List.of(new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3)),
                RANDOM);
        Assertions.assertTrue(check);
    }


//    @Test
//
//    void initialRulesBlockRANDOM() throws Exception {
//        PredictionBlock predictionBlock = new PredictionBlock(  true, 9);
//        boolean check = compareOnnxWithDJL(
//                "./build/PredictionBlock.onnx",
//                predictionBlock,
//                List.of(new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3), new Shape(1, 5, 3, 3)),
//                RANDOM);
//        Assertions.assertTrue(check);
//
//
//
//
//        RepresentationBlock representationBlock = RepresentationBlock.builder().config(config).build();
//        check = compareOnnxWithDJL(
//                "./build/RepresentationBlock.onnx",
//                representationBlock,
//                List.of(new Shape(1, 3, 3, 3)),
//                RANDOM);
//        Assertions.assertTrue(check);
//
//
//        DynamicsBlock dynamicsBlock = DynamicsBlock.newDynamicsBlock(config);
//         check = compareOnnxWithDJL(
//                "./build/DynamicsBlock.onnx",
//                 dynamicsBlock,
//                List.of(new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3), new Shape(1, 80, 3, 3),new Shape(1, 80, 3, 3),new Shape(1, 1, 3, 3)),
//                RANDOM);
//        Assertions.assertTrue(check);
//
//
//        check = compareOnnxWithDJL(
//                "./build/InitialRulesBlock.onnx",
//                new InitialRulesBlock(representationBlock, predictionBlock, dynamicsBlock, config),
//                List.of(new Shape(1, 3, 3, 3)),
//                RANDOM);
//        Assertions.assertTrue(check);
//    }

}
