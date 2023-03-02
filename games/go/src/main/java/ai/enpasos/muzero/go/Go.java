package ai.enpasos.muzero.go;

import ai.enpasos.muzero.go.run.GoArena;
import ai.enpasos.muzero.go.run.GoElo;
import ai.enpasos.muzero.go.run.GoEntropyExtractor;
import ai.enpasos.muzero.go.run.GoLossExtractor;
import ai.enpasos.muzero.go.run.GoOnnx;
import ai.enpasos.muzero.go.run.GoRenderGame;
import ai.enpasos.muzero.go.run.GoStartValueExtractor;
import ai.enpasos.muzero.go.run.GoTrainingAndTest;
import ai.enpasos.muzero.go.run.GoValueExtractor;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.ActionExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@Slf4j
@ComponentScan(basePackages = "ai.enpasos.muzero.*")
public class Go implements CommandLineRunner {

    @Autowired
    private GoTrainingAndTest trainingAndTest;

    @Autowired
    private ActionExtractor actionExtractor;

    @Autowired
    private MuZeroConfig conf;

    @Autowired
    private GoLossExtractor lossExtractor;


    @Autowired
    private GoArena arena;

    @Autowired
    private GoRenderGame renderGame;

    @Autowired
    private GoValueExtractor valueExtractor;

    @Autowired
    private GoEntropyExtractor entropyExtractor;


    @Autowired
    private GoStartValueExtractor startValueExtractor;


    @Autowired
    private GoOnnx onnx;


    @Autowired
    private GoElo elo;

    public static void main(String[] args) {
        SpringApplication.run(Go.class, args);
    }


    @Override
    public void run(String... args) {
        switch (conf.getRun()) {

            case ACTIONS:
                actionExtractor.run();
                break;

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

            case ENTROPY:
                entropyExtractor.run();
                break;
            case STARTVALUES:
                startValueExtractor.run();
                break;
            case ELO:
                elo.run();
                break;


            case NONE:
            default:
        }
        System.exit(0);
    }
}
