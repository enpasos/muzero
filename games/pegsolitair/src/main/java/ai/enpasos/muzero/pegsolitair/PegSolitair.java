package ai.enpasos.muzero.pegsolitair;

import ai.enpasos.muzero.pegsolitair.debug.PegSolitairLossExtractor;
import ai.enpasos.muzero.pegsolitair.debug.PegSolitairOnnx;
import ai.enpasos.muzero.pegsolitair.debug.PegSolitairRenderGame;
import ai.enpasos.muzero.pegsolitair.debug.PegSolitairValueExtractor;
import ai.enpasos.muzero.platform.agent.slow.play.RegularizedPolicyOptimization;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@Slf4j
@ComponentScan(basePackages = "ai.enpasos.muzero.*")
public class PegSolitair implements CommandLineRunner {

    @Autowired
    private PegSolitairTrainingAndTest trainingAndTest;

    @Autowired
    private PegSolitairLossExtractor lossExtractor;

    @Autowired
    private PegSolitairValueExtractor valueExtractor;

    @Autowired
    private PegSolitairRenderGame renderGame;

    @Autowired
    private MuZeroConfig conf;

    @Autowired
    private PegSolitairOnnx onnx;

    @Autowired
    private RegularizedPolicyOptimization regularizedPolicyOptimization;

    public static void main(String[] args) {
        SpringApplication.run(PegSolitair.class, args);
    }


    @Override
    @SuppressWarnings("squid:S125")
    public void run(String... args) {

        switch (conf.getRun()) {
            case TRAIN:
                trainingAndTest.run();
                break;
            case LOSS:
                lossExtractor.run();
                break;
            case ONNX:
                onnx.run();
                break;
            case RENDER:
                renderGame.run();
                break;
            case VALUE:
                valueExtractor.run();
                break;
            case NONE:
            default:
                return;
        }
    }
}