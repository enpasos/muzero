package ai.enpasos.muzero.go;

import ai.enpasos.muzero.go.debug.GoLossExtractor;
import ai.enpasos.muzero.go.debug.GoWinLooseStatistics;
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
public class MuZeroGo implements CommandLineRunner {

    @Autowired
    private TrainingAndTestGo trainingAndTest;


    @Autowired
    private MuZeroConfig conf;


    @Autowired
    GoWinLooseStatistics goWinLooseStatistics;


    @Autowired
    private GoLossExtractor goLossExtractor;

    public static void main(String[] args) {
        SpringApplication.run(MuZeroGo.class, args);
    }


    @Override
    public void run(String... args) {
        trainingAndTest.run();
        // goLossExtractor.run();
        // goWinLooseStatistics.run();
    }
}
