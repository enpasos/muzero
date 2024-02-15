package ai.enpasos.muzero.connect4;


import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.ActionExtractor;
import ai.enpasos.muzero.connect4.run.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@Slf4j
@ComponentScan(basePackages = "ai.enpasos.muzero.*")
@EnableJpaRepositories(basePackages = {"ai.enpasos.muzero.*"})
@EntityScan("ai.enpasos.muzero.*")
public class Connect4 implements CommandLineRunner {

    @Autowired
    private Connect4TrainingAndTest trainingAndTest;

    @Autowired
    private Connect4PolicyOnly policyOnly;

    @Autowired
    private DBService dbService;

    @Autowired
    private Connect4TestComponent test;

    @Autowired
    private Connect4Exploitability exploitability;


    @Autowired
    private Connect4Critical critical;

    @Autowired
    private Connect4Temperature temperature;

    @Autowired
    private Connect4TestAllNetworks testAllNetworks;


    @Autowired
    private Connect4TestAllNetworksExploitability testAllNetworksExploitability;

    @Autowired
    private MuZeroConfig conf;
    @Autowired
    private Connect4LossExtractor goLossExtractor;
    @Autowired
    private Connect4Onnx onnx;
    @Autowired
    private ActionExtractor actionExtractor;

    @Autowired
    private Connect4ValueExtractor valueExtractor;



    @Autowired
    private Connect4FillValueTable fillValueTable;

    @Autowired
    private Connect4FindNetworksDoingABadMove badAction;
    @Autowired
    private Connect4EntropyExtractor entropyExtractor;
    @Autowired
    private Connect4InMindValues inMindValues;

    @Autowired
    private Connect4Inference inference;



    @Autowired
    private ModelService modelService;

    public static void main(String[] args) {
        SpringApplication.run(Connect4.class, args).close();
    }


    @Override
    public void run(String... args) {
        switch (conf.getRun()) {

            case CRITICAL:
                critical.run();
                break;

            case ACTIONS:
                actionExtractor.run();
                break;
            case FILLVALUETABLE:
                fillValueTable.run();
                break;


            case TEMPERATURE:
                temperature.run();
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
            case EXPLOITABILITY:
                exploitability.run();
                break;
            case TESTNETWORKS:
                testAllNetworks.run();
                break;

            case TEST_NETWORKS_EXPLOITABILITY:
                testAllNetworksExploitability.run();
                break;
            case CLEAR_DB:
                dbService.clearDB();
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
//            case REWARD:
//                rewardExtractor.run();
//                break;
            case VALUE:
                valueExtractor.run();
                break;
//            case ENTROPYVALUE:
//                entropyValueExtractor.run();
//                break;
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
