package ai.enpasos.muzero.tictactoe;


import ai.enpasos.muzero.platform.agent.slow.play.RegularizedPolicyOptimization;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.tictactoe.debug.TicTacToeLossExtractor;
import ai.enpasos.muzero.tictactoe.debug.TicTacToeWinLooseStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Slf4j
@ComponentScan(basePackages = "ai.enpasos.muzero.*")
public class TicTacToe implements CommandLineRunner {

    @Autowired
    private TicTacToeTrainingAndTest trainingAndTest;


    @Autowired
    private MuZeroConfig conf;


    @Autowired
    TicTacToeWinLooseStatistics goWinLooseStatistics;


    @Autowired
    private TicTacToeLossExtractor goLossExtractor;

    public static void main(String[] args) {
        SpringApplication.run(TicTacToe.class, args);
    }


    @Override
    public void run(String... args) {
       switch(conf.getRun()) {
           case TRAIN:
               trainingAndTest.run();
               break;
           case LOSS:
               goLossExtractor.run();
               break;
           case RENDER:
               throw new MuZeroException("RENDER not implemented yet.");
           case VALUE:
               throw new MuZeroException("VALUE not implemented yet.");
       }
    }
}
