package ai.enpasos.muzero.pegsolitair;

import ai.enpasos.muzero.pegsolitair.run.PegSolitairLossExtractor;
import ai.enpasos.muzero.pegsolitair.run.PegSolitairOnnx;
import ai.enpasos.muzero.pegsolitair.run.PegSolitairRenderGame;
import ai.enpasos.muzero.pegsolitair.run.PegSolitairTrainingAndTest;
import ai.enpasos.muzero.pegsolitair.run.PegSolitairValueExtractor;
import ai.enpasos.muzero.platform.agent.c_model.service.ModelService;
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
    private ModelService modelService;


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
        }

       modelService.shutdown();
    }
}
