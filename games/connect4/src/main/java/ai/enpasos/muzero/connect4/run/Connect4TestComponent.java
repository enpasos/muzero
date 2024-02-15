package ai.enpasos.muzero.connect4.run;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.connect4.run.test.BadDecisions;
import ai.enpasos.muzero.connect4.run.test.Connect4Test;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class Connect4TestComponent {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private Connect4Test ticTacToeTest;

    @Autowired
    private MuZeroLoop muZero;

    public void run() {
         BadDecisions bd = ticTacToeTest.findBadDecisions();
        boolean passed = bd.total() == 0;
        String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
        log.info(message);

    }


}
