package ai.enpasos.muzero.tictactoe;


import ai.djl.Device;
import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.BaseNDManager;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.MuZero;
import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.djl.MyCheckpointsTrainingListener;
import ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.tictactoe.config.TicTacToeConfigFactory;
import ai.enpasos.muzero.tictactoe.test.TicTacToeTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import static ai.enpasos.muzero.platform.MuZero.getNetworksBasedir;
import static ai.enpasos.muzero.platform.MuZero.train;
import static ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper.*;

@Slf4j
public class TrainingAndTest {

    public static void main(String[] args) throws URISyntaxException, IOException {
        MuZeroConfig config = TicTacToeConfigFactory.getTicTacToeInstance();

        String dir = "./memory/";
        config.setOutputDir(dir);

        FileUtils.deleteDirectory(new File(dir));

        boolean freshBuffer = false;
        int numberOfEpochs = 1;

        train(config, freshBuffer, numberOfEpochs);

        boolean passed = TicTacToeTest.test(config);
        String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
        log.info(message);
        if (!passed) throw new RuntimeException(message);
    }




}
