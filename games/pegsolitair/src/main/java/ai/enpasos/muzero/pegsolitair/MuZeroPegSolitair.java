package ai.enpasos.muzero.pegsolitair;

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
public class MuZeroPegSolitair implements CommandLineRunner {

    @Autowired
    private TrainingAndTestPegSolitair trainingAndTest;

//    @Autowired
//    private PegSolitairLossExtractor lossExtractor;

    @Autowired
    private MuZeroConfig conf;

    @Autowired
    private RegularizedPolicyOptimization regularizedPolicyOptimization;

    public static void main(String[] args) {
        SpringApplication.run(MuZeroPegSolitair.class, args);
    }



    @Override
    public void run(String... args) {
         trainingAndTest.run();
    }
}
