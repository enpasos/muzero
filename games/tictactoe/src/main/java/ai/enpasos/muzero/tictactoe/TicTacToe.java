package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.slow.play.RegularizedPolicyOptimization;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.tictactoe.debug.TicTacToeLossExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@Slf4j
@ComponentScan(basePackages = "ai.enpasos.muzero.*")
public class TicTacToe implements CommandLineRunner {

    @Autowired
    private TicTacToeTrainingAndTest trainingAndTest;

    @Autowired
    private TicTacToeLossExtractor ticTacToeLossExtractor;

    @Autowired
    private MuZeroConfig conf;

    @Autowired
    private RegularizedPolicyOptimization regularizedPolicyOptimization;

    public static void main(String[] args) {
        SpringApplication.run(TicTacToe.class, args);
    }


    @Override
    @SuppressWarnings("squid:S125")
    public void run(String... args) {
         trainingAndTest.run();
        // ticTacToeLossExtractor.run();


    }
}
