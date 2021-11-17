package ai.enpasos.muzero.tictactoe;


import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.tictactoe.config.TicTacToeConfigFactory;
import ai.enpasos.muzero.tictactoe.test.TicTacToeTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static ai.enpasos.muzero.platform.MuZero.train;

@Slf4j
public class TrainingAndTest {

    public static void main(String[] args) throws URISyntaxException, IOException {
        MuZeroConfig config = TicTacToeConfigFactory.getTicTacToeInstance();

        String dir = "./memory/";
        config.setOutputDir(dir);

     //   FileUtils.deleteDirectory(new File(dir));

        boolean freshBuffer = false;
        int numberOfEpochs = 1;

        train(config, freshBuffer, numberOfEpochs);

        boolean passed = TicTacToeTest.test(config);
        String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
        log.info(message);
        if (!passed) throw new RuntimeException(message);

    }




}
