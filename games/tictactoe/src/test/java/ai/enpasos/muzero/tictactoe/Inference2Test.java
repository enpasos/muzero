package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.b_episode.SelfPlayGame;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelQueue;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
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

import static ai.enpasos.muzero.platform.common.Functions.nonSimilarity;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class })
@Slf4j
@Disabled
class Inference2Test {

    @Autowired
    PlayService multiGameStarter;


    @Autowired
    MuZeroConfig config;


    @Autowired
    MuZeroLoop muzero;


    @Autowired
    SelfPlayGame selfPlayer;

    @Autowired
    GameBuffer replayBuffer;



    @Autowired
    BatchFactory batchFactory;

    @Autowired
    ModelQueue inferenceQueue;


    @Autowired
    ModelService modelService;






    @Test
    void testInference2() throws ExecutionException, InterruptedException {

        init();
        Game game0 = config.newGame(true, true);
        Game game1 = config.newGame(true, true);
        Game game2 = config.newGame(true, true);
        Game game3 = config.newGame(true, true);
        game1.apply(4);
        game3.apply(3, 4, 5, 6, 7, 8);
        List<NetworkIO> networkIOList = modelService.initialInference2(List.of(game0, game1, game2, game3)).get();
        double s_0_2 = nonSimilarity(networkIOList.get(0).getSimilarityVector(), networkIOList.get(2).getSimilarityVector());
        double s_1_2 = nonSimilarity(networkIOList.get(1).getSimilarityVector(), networkIOList.get(2).getSimilarityVector());
        double s_0_1 = nonSimilarity(networkIOList.get(0).getSimilarityVector(), networkIOList.get(1).getSimilarityVector());
        double s_0_3 = nonSimilarity(networkIOList.get(0).getSimilarityVector(), networkIOList.get(3).getSimilarityVector());
        double s_1_3 = nonSimilarity(networkIOList.get(1).getSimilarityVector(), networkIOList.get(3).getSimilarityVector());
        double s_2_3 = nonSimilarity(networkIOList.get(2).getSimilarityVector(), networkIOList.get(3).getSimilarityVector());

        log.info("s_0_2: {}", s_0_2);
        log.info("s_1_2: {}", s_1_2);
        log.info("s_0_1: {}", s_0_1);
        log.info("s_0_3: {}", s_0_3);
        log.info("s_1_3: {}", s_1_3);
        log.info("s_2_3: {}", s_2_3);

       // assertNotNull(networkIOList);
        // assertNotNull(networkIO.getHiddenState());
       // assertNotNull(networkIO.getHiddenState());

    }

    private void init() throws InterruptedException, ExecutionException {

      //  config.setOutputDir("./build/tictactoeTest/");
        config.setOutputDir("../../memory/tictactoe/");
     //   rmDir(config.getOutputDir());
        modelService.loadLatestModelOrCreateIfNotExisting().get();
    }


}
