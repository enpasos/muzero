package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.TrainParams;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.b_episode.Play;
import ai.enpasos.muzero.platform.agent.b_episode.SelfPlayGame;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelQueue;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueRepo;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import ai.enpasos.muzero.platform.config.TrainingTypeKey;
import ai.enpasos.muzero.platform.run.FillValueTable;
import ai.enpasos.muzero.platform.run.TemperatureCalculator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class})
@Slf4j
class InferenceTest2 {

    @Autowired
    MuZeroConfig config;
    @Autowired
    GameBuffer gameBuffer;
    @Autowired
    ModelService modelService;
    @Autowired
    Play play;
    @Autowired
    ModelState modelState;
    @Autowired
    ValueRepo valueRepo;
    @Autowired
    FillValueTable fillValueTable;
    @Autowired
    TemperatureCalculator temperatureCalculator;
    @Autowired
    PlayService multiGameStarter;
    @Autowired
    MuZeroLoop muzero;
    @Autowired
    SelfPlayGame selfPlayer;
    @Autowired
    GameBuffer replayBuffer;
    @Autowired
    BatchFactory batchFactory;
    @Autowired
    ModelQueue inferenceQueue;

    @Test
    void testInference() throws ExecutionException, InterruptedException {

        init();
        Game game = config.newGame(true, true);
        List<NetworkIO> networkIO = modelService.initialInference(List.of(game), 0, 1).get();
//        Node node = Node.builder()
//                .hiddenState(networkIO.getHiddenState())
//                .build();
//        Node node2 = Node.builder()
//                .action(config.newAction(4))
//                .build();
//        NetworkIO networkIO2 = modelService.recurrentInference(List.of(node, node2)).get();
//
//        assertNotNull(networkIO.getHiddenState());
//        assertNotNull(networkIO2.getHiddenState());

    }

    private void init() throws InterruptedException, ExecutionException {
        config.setOutputDir("./build/tictactoeTest/");
        rmDir(config.getOutputDir());
       // modelService.loadLatestModelOrCreateIfNotExisting().get();

        TrainParams params = TrainParams.builder().build();

            int trainingStep = 0;
            int epoch = 0;

            List<DurAndMem> durations = new ArrayList<>();

            modelService.loadLatestModelOrCreateIfNotExisting().get();
            epoch = modelState.getEpoch();

            gameBuffer.loadLatestStateIfExists();
            play.fillingBuffer(params.isRandomFill());


      //      while (trainingStep < config.getNumberOfTrainingSteps()) {

                DurAndMem duration = new DurAndMem();
                duration.on();

                int n = 10;  // TODO: make configurable

                if (epoch != 0) {

                    log.info("reflecting on experience ...");
                    fillValueTable.fillValueTableForNetworkOfEpoch(epoch);
                    temperatureCalculator.aggregateValueStatisticsUp(epoch, n);
                    temperatureCalculator.markArchived(epoch);

                    log.info("collecting experience ...");
                    PlayTypeKey originalPlayTypeKey = config.getPlayTypeKey();
                    for (PlayTypeKey key : config.getPlayTypeKeysForTraining()) {
                        config.setPlayTypeKey(key);
                        play.playGames(params.isRender(), trainingStep, epoch);
                    }
                    config.setPlayTypeKey(originalPlayTypeKey);
                }



                log.info("game counter: " + gameBuffer.getBuffer().getCounter());
                log.info("window size: " + gameBuffer.getBuffer().getWindowSize());
                log.info("gameBuffer size: " + this.gameBuffer.getBuffer().getGames().size());

                log.info("training ...");
                //  modelService.trainModel(TrainingTypeKey.RULES).get();
                modelService.trainModel(TrainingTypeKey.POLICY_VALUE).get();

                epoch = modelState.getEpoch();

                trainingStep = epoch * config.getNumberOfTrainingStepsPerEpoch();

                duration.off();
                durations.add(duration);
                System.out.println("epoch;duration[ms];gpuMem[MiB]");
                IntStream.range(0, durations.size()).forEach(k -> System.out.println(k + ";" + durations.get(k).getDur() + ";" + durations.get(k).getMem() / 1024 / 1024));

         //   }

        }



}
