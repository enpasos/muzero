package ai.enpasos.muzero.tictactoe;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.b_planning.PlayParameters;
import ai.enpasos.muzero.platform.agent.b_planning.service.PlayService;
import ai.enpasos.muzero.platform.agent.c_model.Network;
import ai.enpasos.muzero.platform.agent.d_experience.Game;
import ai.enpasos.muzero.platform.agent.d_experience.GameBuffer;
import ai.enpasos.muzero.platform.config.FileType;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.train.MuZero;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class ProtobufferTest {


    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    MuZeroConfig config;

    @Autowired
    MuZero muZero;


    @Autowired
    PlayService playService;

    @Test
    void writeAndReadZippedJsonTest() {
        config.setGameBufferWritingFormat(FileType.ZIPPED_JSON);
        writeAndReadTest();
    }

    @Test
    void writeAndReadProtoBufTest() {
        config.setGameBufferWritingFormat(FileType.ZIPPED_PROTOCOL_BUFFERS);
        writeAndReadTest();
    }

    private void writeAndReadTest() {
        config.setOutputDir("./build/tictactoeTest/");
        muZero.deleteNetworksAndGames();
        try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
            Network network = new Network(config, model);
            List<Game> games =   playService.playNewGames( 1,
                PlayParameters.builder()
                    .render(false)
                    .fastRulesLearning(true)
                    .justInitialInferencePolicy(false)
                    .build());
           // List<Game> games = selfPlay.playGame( network, false, true, false);
            gameBuffer.init();

            gameBuffer.getGameBufferIO().saveGames(games, network.getModel().getName(), config);

            List<Game> gamesOld = gameBuffer.getBuffer().getGames();


            IntStream.range(0, gamesOld.size()).forEach(i -> assertEquals(gamesOld.get(i), gameBuffer.getBuffer().getGames().get(i), "games should be the same"));
        }
    }
}
