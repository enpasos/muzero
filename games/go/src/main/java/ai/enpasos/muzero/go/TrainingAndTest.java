package ai.enpasos.muzero.go;

import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.go.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;

import static ai.enpasos.muzero.platform.MuZero.train;

@Slf4j
public class TrainingAndTest {

    public static void main(String[] args) throws URISyntaxException, IOException {
        MuZeroConfig config = ConfigFactory.getGoInstance(5);
        String dir = "./memory/";
        config.setOutputDir(dir);

//        FileUtils.deleteDirectory(new File(dir));

        train(config);
      //  boolean passed = TicTacToeTest.test(config);
//        String message = "INTEGRATIONTEST = " + (passed ? "passed": "failed");
//        log.info(message);
//        if (!passed) throw new RuntimeException(message);
    }


}
