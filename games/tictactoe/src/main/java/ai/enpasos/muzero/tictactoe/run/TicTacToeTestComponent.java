package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.run.MuZero;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class TicTacToeTestComponent {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private ai.enpasos.muzero.tictactoe.test.TicTacToeTest ticTacToeTest;

    @Autowired
    private MuZero muZero;

    public void run() {

        boolean passed = ticTacToeTest.test();
        String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
        log.info(message);

    }


}
