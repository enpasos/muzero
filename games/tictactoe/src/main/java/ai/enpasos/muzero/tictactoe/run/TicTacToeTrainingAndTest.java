package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.TrainParams;
import ai.enpasos.muzero.tictactoe.run.test.BadDecisions;
import ai.enpasos.muzero.tictactoe.run.test.TicTacToeTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;


@Slf4j
@Component
public class TicTacToeTrainingAndTest {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private TicTacToeTest ticTacToeTest;

    @Autowired
    private MuZeroLoop muZero;

    @SuppressWarnings({"java:S2583", "java:S2589"})
    public void run() {


        // TODO separate model and memory from scratch

        boolean deleteModel = true;

        if (deleteModel) {
            rmDir(config.getOutputDir());
        }

        try {
            muZero.train(TrainParams.builder()
                .render(true)
                .build());
        } catch (InterruptedException e) {
            throw new MuZeroException(e);
        } catch (ExecutionException e) {
            throw new MuZeroException(e);
        }
        BadDecisions bd = ticTacToeTest.findBadDecisions();
        boolean passed = bd.total() == 0;
        String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
        log.info(message);
    }


}
