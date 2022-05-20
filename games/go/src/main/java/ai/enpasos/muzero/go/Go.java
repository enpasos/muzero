package ai.enpasos.muzero.go;

import ai.enpasos.muzero.go.run.*;
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
public class Go implements CommandLineRunner {

    @Autowired
    private GoTrainingAndTest trainingAndTest;


    @Autowired
    private GoTrainFastAndTest trainFastAndTest;

    @Autowired
    private MuZeroConfig conf;

    @Autowired
    private GoLossExtractor lossExtractor;

    @Autowired
    private GoWinLooseStatistics goWinLooseStatistics;

    @Autowired
    private GoArena arena;

    @Autowired
    private GoRenderGame renderGame;

    @Autowired
    private GoValueExtractor valueExtractor;
    @Autowired
    private GoValuesExtractor valuesExtractor;
    @Autowired
    private GoEntropyExtractor entropyExtractor;


    @Autowired
    private GoStartValueExtractor startValueExtractor;

    @Autowired
    private GoWinLooseStatistics winLooseStatistics;

    @Autowired
    private GoSurpriseExtractor goSurpriseExtractor;

    @Autowired
    private GoSurprise goSurprise;
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
            case TRAINFAST:
                trainFastAndTest.run();
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
            case VALUES:
                valuesExtractor.run();
                break;
            case ENTROPY:
                entropyExtractor.run();
                break;
            case STARTVALUES:
                startValueExtractor.run();
                break;
            case WINLOOSE:
                winLooseStatistics.run();
                break;
            case ELO:
                elo.run();
                break;
            case SURPRISEEXTRACT:
                goSurpriseExtractor.run();
                break;
            case SURPRISE:
                goSurprise.run();
                break;
            case NONE:
            default:
                return;
        }
    }
}
