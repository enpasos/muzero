package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.b_episode.PlayParameters;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.b_episode.SelfPlayGame;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelQueue;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import lombok.extern.slf4j.Slf4j;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class })
@Slf4j
class PlayTest {

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
    void testMultiGameStarter_Basic() throws ExecutionException, InterruptedException {
        init();
         int n = 1000;
            List<Game> games = multiGameStarter.playNewGames( n,
                PlayParameters.builder()
                    .render(false)
                    .build());
            assertEquals(n, games.size());
            assertTrue(inferenceQueue.getInitialInferenceTasks().isEmpty());
     }

    @Test
    void testMultiGameStarter_FastRule() throws ExecutionException, InterruptedException {
        init();
        int n = 1000;
        List<Game> games = multiGameStarter.playNewGames( n,
            PlayParameters.builder()
                .render(false)
                .fastRulesLearning(true)
                .build());
        assertEquals(n, games.size());
        assertTrue(inferenceQueue.getInitialInferenceTasks().isEmpty());
    }

    private void init() throws InterruptedException, ExecutionException {
        config.setOutputDir("./build/tictactoeTest/");

        rmDir(config.getOutputDir());
        modelService.loadLatestModelOrCreateIfNotExisting().get();
    }


}
