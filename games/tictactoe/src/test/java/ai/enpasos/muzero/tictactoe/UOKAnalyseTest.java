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

import java.util.Arrays;
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
        testAGame( 3, 4,0,3,5,2,6,7,1,8);
        testAGame(3, 3,0,4,1,5);
        testAGame( 9, 4,0,3,5,2,6,7,1,8);
        testAGame(5, 3,0,4,1,5);
    }

    private void testAGame(int unrollSteps, int... a) throws ExecutionException, InterruptedException {
        init();

        Game game = config.newGame(true, true);
        game.apply(a);

        selfPlayGame.uOkAnalyseGame(game,  true);
        List<Integer> uOks = game.getEpisodeDO().getTimeSteps().stream().mapToInt(ts -> ts.getUOk()).boxed().collect(Collectors.toList());
        log.info("uOks: " + uOks);
        Integer[] uOKExpected = new Integer[a.length + 1];
        Arrays.fill(uOKExpected, -1);
        assertEquals(uOKExpected.length  , uOks.size());
        assertArrayEquals( uOKExpected, uOks.toArray(new Integer[0]));
    }

    private void init() throws InterruptedException, ExecutionException {
        config.setOutputDir("./build/tictactoeTest/");
        rmDir(config.getOutputDir());
        modelService.loadLatestModelOrCreateIfNotExisting().get();
    }


}
