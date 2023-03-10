package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZero;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.TrainParams;
import ai.enpasos.muzero.tictactoe.run.test.TicTacToeTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;


@Slf4j
@Component
public class TicTacToeTrainingAndTest {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private TicTacToeTest ticTacToeTest;

    @Autowired
    private MuZero muZero;

    @SuppressWarnings({"java:S2583", "java:S2589"})
    public void run() {

        boolean startFromScratch = true;

        if (startFromScratch) {
            rmDir(config.getOutputDir());
        }
        try {
            muZero.train(TrainParams.builder()
                .render(true)
                .withoutFill(!startFromScratch)
                .build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean passed = ticTacToeTest.findBadDecisions() == 0;
        String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
        log.info(message);
    }


}
