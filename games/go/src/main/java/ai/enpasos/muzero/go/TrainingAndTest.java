package ai.enpasos.muzero.go;

import ai.enpasos.muzero.go.config.GoConfigFactory;
import ai.enpasos.muzero.platform.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;

import static ai.enpasos.muzero.platform.MuZero.train;

@Slf4j
public class TrainingAndTest {

    public static void main(String[] args) throws URISyntaxException, IOException {
        int size = 5;

        MuZeroConfig config = GoConfigFactory.getGoInstance(size);
        String dir = "./memory/go"+ size + "/";
        config.setOutputDir(dir);

//        FileUtils.deleteDirectory(new File(dir));

        train(config, true);
        //  boolean passed = TicTacToeTest.test(config);
//        String message = "INTEGRATIONTEST = " + (passed ? "passed": "failed");
//        log.info(message);
//        if (!passed) throw new RuntimeException(message);
    }


}
