package ai.enpasos.muzero.tictactoe;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.b_episode.Play;
import ai.enpasos.muzero.platform.agent.b_episode.PlayParameters;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.Network;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.GameBufferDTO;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class ProtobufferTest {


    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    MuZeroConfig config;

    @Autowired
    MuZeroLoop muZero;

    @Autowired
    private ModelState modelState;

    @Autowired
    Play play;


    @Autowired
    PlayService playService;



    @Test
    void writeAndReadProtoBufTest() {

        writeAndReadTest();
    }

    @Test
    void writeAndReadProtoBuf2Test() {

        writeAndRead2Test();
    }

    private void writeAndReadTest() {
        gameBuffer.init();
        config.setOutputDir("./build/tictactoeTest/");
        play.deleteNetworksAndGames();
        try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
            Network network = new Network(config, model);
            List<Game> games = playService.playNewGames(1,
                    PlayParameters.builder()
                            .render(false)
                            .fastRulesLearning(true)
                            .justInitialInferencePolicy(false)
                            .build());
           // System.out.println(games.get(0).getGameDTO().getActions());
            modelState.setEpoch(10);
            gameBuffer.addGames(games, false);

            GameBufferDTO dtoOriginal = gameBuffer.getBuffer();

            gameBuffer.setBuffer(null);
            gameBuffer.loadLatestStateIfExists();
            GameBufferDTO dtoNew = gameBuffer.getBuffer();
            assertTrue(dtoOriginal.deepEquals(dtoNew), "game buffers should be the same");

        }
    }

    private void writeAndRead2Test() {
        gameBuffer.init();
        config.setOutputDir("./build/tictactoeTest2/");
        play.deleteNetworksAndGames();
        try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
            Network network = new Network(config, model);
            Game game = config.newGame(true, true);
            game.apply(5, 6, 2, 0, 3, 4, 1, 7, 8);
            List<Game> games = List.of(game);

            modelState.setEpoch(10);
            gameBuffer.addGames(games, false);

            GameBufferDTO dtoOriginal = gameBuffer.getBuffer();
            gameBuffer.setBuffer(null);
            gameBuffer.loadLatestStateIfExists();
            GameBufferDTO dtoNew = gameBuffer.getBuffer();
            assertTrue(dtoOriginal.deepEquals(dtoNew), "game buffers should be the same");

        }
    }
}
