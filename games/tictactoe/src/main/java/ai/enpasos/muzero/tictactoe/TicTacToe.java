package ai.enpasos.muzero.tictactoe;


import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.ActionExtractor;
import ai.enpasos.muzero.tictactoe.run.*;
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
    private TicTacToePolicyOnly policyOnly;

    @Autowired
    private TicTacToeTestComponent test;

    @Autowired
    private TicTacToeTestAllNetworks testAllNetworks;

    @Autowired
    private MuZeroConfig conf;
    @Autowired
    private TicTacToeLossExtractor goLossExtractor;
    @Autowired
    private TicTacToeOnnx onnx;
    @Autowired
    private ActionExtractor actionExtractor;

    @Autowired
    private TicTacToeValueExtractor valueExtractor;

    @Autowired
    private TicTacToeEntropyValueExtractor entropyValueExtractor;

    @Autowired
    private TicTacToeFindNetworksDoingABadMove badAction;
    @Autowired
    private TicTacToeEntropyExtractor entropyExtractor;
    @Autowired
    private TicTacToeInMindValues inMindValues;

    @Autowired
    private TicTacToeInference inference;

    @Autowired
    private ModelService modelService;

    public static void main(String[] args) {
        SpringApplication.run(TicTacToe.class, args).close();
    }


    @Override
    public void run(String... args) {
        switch (conf.getRun()) {
            case ACTIONS:
                actionExtractor.run();
                break;
            case INMIND:
                inMindValues.run();
                break;
            case BADACTION:
                badAction.run();
                break;
            case TRAIN:
                trainingAndTest.run();
                break;
            case POLICYONLY:
                policyOnly.run();
                break;
            case TEST:
                test.run();
                break;
            case TESTNETWORKS:
                testAllNetworks.run();
                break;
            case LOSS:
                goLossExtractor.run();
                break;
            case ONNX:
                onnx.run();
                break;
            case INFERENCE:
                inference.run();
                break;
            case RENDER:
                throw new MuZeroException("RENDER not implemented yet.");
            case VALUE:
                valueExtractor.run();
                break;
            case ENTROPYVALUE:
                entropyValueExtractor.run();
                break;
            case ENTROPY:
                entropyExtractor.run(false);
                break;
            case ENTROPY0:
                entropyExtractor.run(true);
                break;
            case NONE:
                return;
            default:
        }
        modelService.shutdown();

    }
}
