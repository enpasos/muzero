package ai.enpasos.muzero.go;

import ai.enpasos.muzero.go.debug.*;
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
    private GoWinLooseStatistics winLooseStatistics;

    @Autowired
    private GoOnnx onnx;

    public static void main(String[] args) {
        SpringApplication.run(Go.class, args);
    }


    @Override
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
            case WINLOOSE:
                winLooseStatistics.run();
                break;
            case NONE:
            default:
                return;
        }
    }
}
