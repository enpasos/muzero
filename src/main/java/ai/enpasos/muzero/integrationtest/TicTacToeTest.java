package ai.enpasos.muzero.integrationtest;

import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static ai.enpasos.muzero.MuZero.*;
import static ai.enpasos.muzero.debug.TicTacToeTest.test;

@Slf4j
public class TicTacToeTest {

    public static void main(String[] args) throws URISyntaxException, IOException {
        MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();
        String dir = "./memory/integrationtest/tictactoe/";
        FileUtils.deleteDirectory(new File(dir));
        config.setOutputDir(dir); 
        train(config);
        boolean passed = test(config);
        String message = "INTEGRATIONTEST = " + (passed ? "passed": "failed");
        log.info(message);
        if (!passed) throw new RuntimeException(message);
    }

    public static void train(MuZeroConfig config) {
        createNetworkModelIfNotExisting(config);

        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        replayBuffer.loadLatestState();
        initialFillingBuffer(config, replayBuffer);

        int trainingStep = NetworkHelper.numberOfLastTrainingStep(config);

        while (trainingStep < config.getNumberOfTrainingSteps()) {
            if (trainingStep != 0) {
                log.info("last training step = {}", trainingStep);
                log.info("numSimulations: " + config.getNumSimulations());
                playOnDeepThinking(config, replayBuffer);
                replayBuffer.saveState();
            }
            trainingStep = NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 1);
         }

    }
}
