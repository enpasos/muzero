package ai.enpasos.muzero.go;

import ai.enpasos.muzero.go.debug.*;
import ai.enpasos.muzero.platform.common.MuZeroException;
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

    public static void main(String[] args) {
        SpringApplication.run(Go.class, args);
    }


    @Override
    public void run(String... args) {
         switch(conf.getRun()) {
            case TRAIN:
                trainingAndTest.run();
                break;
            case LOSS:
                lossExtractor.run();
                break;
            case RENDER:
                renderGame.run();
            case VALUE:
                valueExtractor.run();
        }
    }
}
