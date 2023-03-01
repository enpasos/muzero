package ai.enpasos.muzero.tictactoe;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.GameBuffer;
import ai.enpasos.muzero.platform.agent.rational.Node;
import ai.enpasos.muzero.platform.agent.rational.async.GlobalState;
import ai.enpasos.muzero.platform.agent.rational.async.ModelQueue;
import ai.enpasos.muzero.platform.agent.rational.async.ModelService;
import ai.enpasos.muzero.platform.agent.rational.async.ParallelEpisodesStarter;
import ai.enpasos.muzero.platform.agent.rational.async.PlayParameters;
import ai.enpasos.muzero.platform.agent.rational.async.play.SelfPlayGame;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.train.MuZero;
import ai.enpasos.muzero.platform.run.train.TrainParams;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class })
@Slf4j
class AsyncTest {

    @Autowired
    ParallelEpisodesStarter multiGameStarter;


    @Autowired
    MuZeroConfig config;


    @Autowired
    MuZero muzero;


    @Autowired
    SelfPlayGame selfPlayer;

    @Autowired
    GameBuffer replayBuffer;



    @Autowired
    NetworkHelper networkHelper;

    @Autowired
    ModelQueue inferenceQueue;


    @Autowired
    ModelService modelService;

    @Autowired
    GlobalState globalState;


    @Test
    void testMultiGameStarter() throws ExecutionException, InterruptedException {
        config.setOutputDir("./build/tictactoeTest/");
        rmDir(config.getOutputDir());
        modelService.loadLatestModelOrCreateIfNotExisting().get();
//        TrainParams params = TrainParams.builder()
//            .render(false)
//            .randomFill(false)
//            .withoutFill(true)
//            .build();

            int n = 1000;
            List<Game> games = multiGameStarter.playNewGames( n,
                PlayParameters.builder()
                    .render(false)
                    .build());
            assertEquals(n, games.size());
            assertTrue(inferenceQueue.getInitialInferenceTasks().isEmpty());


    }


//    @Test
//    @Disabled
//    void testInference2() throws ExecutionException, InterruptedException {
//        modelService.loadLatestModel().get();
//        Game game = config.newGame();
//        NetworkIO networkIO = modelService.initialInference(game).get();
//        Node node = Node.builder()
//            .hiddenState(networkIO.getHiddenState())
//            .build();
//        Node node2 = Node.builder()
//            .action(config.newAction(4))
//            .build();
//        NetworkIO networkIO2 = modelService.recurrentInference(List.of(node, node2)).get();
//
//        assertNotNull(networkIO.getHiddenState());
//        assertNotNull(networkIO2.getHiddenState());
//    }

    @Test
    void testInference() throws ExecutionException, InterruptedException {
      //  try {
            config.setOutputDir("./build/tictactoeTest/");
            rmDir(config.getOutputDir());
            modelService.loadLatestModelOrCreateIfNotExisting().get();
            Game game = config.newGame();
            NetworkIO networkIO = modelService.initialInference(game).get();
            Node node = Node.builder()
                .hiddenState(networkIO.getHiddenState())
                .build();
            Node node2 = Node.builder()
                .action(config.newAction(4))
                .build();
            NetworkIO networkIO2 = modelService.recurrentInference(List.of(node, node2)).get();

            assertNotNull(networkIO.getHiddenState());
            assertNotNull(networkIO2.getHiddenState());


//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


//    @Test
//    void testSelfplayer() {
//        TrainParams params = TrainParams.builder()
//            //.render(true)
//            .randomFill(false)
//            .withoutFill(true)
//            .build();
//        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
//            Network network = new Network(config, model);
//            globalState.setNetwork(network);
//            muzero.init(params.isFreshBuffer(), params.isRandomFill(), network, params.withoutFill);
//            try (NDManager nDManager = network.getNDManager().newSubManager()) {
//                List<NDArray> actionSpaceOnDevice = Network.getAllActionsOnDevice(config, nDManager);
//                network.setActionSpaceOnDevice(actionSpaceOnDevice);
//                network.createAndSetHiddenStateNDManager(nDManager, true);
//
//                Game game = config.newGame();
//                selfPlayer.play( game, PlayParameters.builder().build());
//
//            }
//            assertTrue(inferenceQueue.getInitialInferenceTasks().isEmpty());
//        }
//
//    }


}
