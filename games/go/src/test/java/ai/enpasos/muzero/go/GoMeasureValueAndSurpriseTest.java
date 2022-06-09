package ai.enpasos.muzero.go;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.training.DefaultTrainingConfig;
import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.Surprise;
import ai.enpasos.muzero.platform.run.train.MuZero;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class GoMeasureValueAndSurpriseTest {

    @Autowired
    MuZeroConfig config;

    @Autowired
    MuZero muzero;


    @Autowired
    ReplayBuffer replayBuffer;

    @Autowired
    Surprise surprise;

    @Autowired
    NetworkHelper networkHelper;

    @Test
    void someTest() {
        config.setNetworkBaseDir("./pretrained");
        config.setOutputDir("./src/test/resources/");
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            Network network = new Network(config, model);
            muzero.init(false, false, network, true);

//
            int epoch = networkHelper.getEpoch();
            int trainingStep = config.getNumberOfTrainingStepsPerEpoch() * epoch;
            DefaultTrainingConfig djlConfig = networkHelper.setupTrainingConfig(epoch);

            List<Game> games = this.replayBuffer.getBuffer().getGames();


            int loop = 0;

            double surpriseThreshold = Double.MAX_VALUE;

            //log.info("*>*>*>* looping " + loop++);

            log.info("start surprise.measureValueAndSurprise");
            surprise.measureValueAndSurprise(network, games);
            int i = 42;


        }

    }


}
