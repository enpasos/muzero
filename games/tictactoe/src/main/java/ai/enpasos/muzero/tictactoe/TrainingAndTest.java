package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.MuZero;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static ai.enpasos.muzero.MuZero.train;

@Slf4j
public class TrainingAndTest {

    public static void main(String[] args) throws URISyntaxException, IOException {
        MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();
        String dir = "./memory/";
        config.setOutputDir(dir);

//        FileUtils.deleteDirectory(new File(dir));

        train(config);
        boolean passed = ai.enpasos.muzero.tictactoe.debug.TicTacToeTest.test(config);
        String message = "INTEGRATIONTEST = " + (passed ? "passed": "failed");
        log.info(message);
        if (!passed) throw new RuntimeException(message);
    }


}
