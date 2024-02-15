package ai.enpasos.muzero.connect4.run;


import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.TrainParams;
import ai.enpasos.muzero.connect4.run.test.BadDecisions;
import ai.enpasos.muzero.connect4.run.test.Connect4Test;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;


@Slf4j
@Component
public class Connect4TrainingAndTest {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private Connect4Test ticTacToeTest;

    @Autowired
    private MuZeroLoop muZero;

    @SuppressWarnings({"java:S2583", "java:S2589"})
    public void run() {


        // TODO separate model and memory from scratch

        boolean deleteModel = false;

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
