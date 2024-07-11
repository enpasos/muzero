package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.b_episode.PlayParameters;
import ai.enpasos.muzero.platform.agent.b_episode.SelfPlayGame;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelQueue;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
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
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class })
@Slf4j
class UOKAnalyseTest {


    @Autowired
    MuZeroConfig config;

    @Autowired
    SelfPlayGame selfPlayer;

    @Autowired
    ModelService modelService;

    @Autowired
    SelfPlayGame selfPlayGame;



    @Test
    void analyseTest() throws ExecutionException, InterruptedException {
        init();

        Game game = config.newGame(true, true);
        game.apply(4,0,3,5,2,6,7,1,8);

        selfPlayGame.uOkAnalyseGame(game, 3);
        List<Integer> uOks = game.getEpisodeDO().getTimeSteps().stream().mapToInt(ts -> ts.getUOk()).boxed().collect(Collectors.toList());
        log.info("uOks: " + uOks);
        assertEquals(10, uOks.size());
        assertArrayEquals(new Integer[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, uOks.toArray(new Integer[0]));
    }

    private void init() throws InterruptedException, ExecutionException {
        config.setOutputDir("./build/tictactoeTest/");
        rmDir(config.getOutputDir());
        modelService.loadLatestModelOrCreateIfNotExisting().get();
    }


}
