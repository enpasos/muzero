package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.tictactoe.run.test.BadDecisions;
import ai.enpasos.muzero.tictactoe.run.test.TicTacToeTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class TicTacToeTestComponent {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private TicTacToeTest ticTacToeTest;

    @Autowired
    private MuZeroLoop muZero;

    public void run() {
         BadDecisions bd = ticTacToeTest.findBadDecisions();
        boolean passed = bd.total() == 0;
        String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
        log.info(message);

    }


}
