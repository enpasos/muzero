package ai.enpasos.muzero.tictactoe;


import ai.enpasos.muzero.platform.MuZero;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.tictactoe.test.TicTacToeTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;


@Slf4j
@Component
public class TrainingAndTestTicTacToe {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private TicTacToeTest ticTacToeTest;

    @Autowired
    private MuZero muZero;

    public void run()  {

        String dir = "./memory/tictactoe/";
        config.setOutputDir(dir);

        try {
            FileUtils.deleteDirectory(new File(dir));
        } catch (Exception e) {
            throw new MuZeroException(e);
        }

        muZero.train(false, 1);

        boolean passed = ticTacToeTest.test();
        String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
        log.info(message);

    }



}
