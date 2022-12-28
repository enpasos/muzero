package ai.enpasos.muzero.tictactoe;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
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
    ReplayBuffer replayBuffer;

    @Autowired
    MuZeroConfig config;

    @Autowired
    MuZero muZero;

    @Autowired
    SelfPlay selfPlay;

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
        config.setOutputDir("./memory/tictactoeTest/");
        muZero.deleteNetworksAndGames();
        try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
            Network network = new Network(config, model);
            List<Game> games = selfPlay.playGame( network, false, true, false, false);
            replayBuffer.init();

            replayBuffer.getReplayBufferIO().saveGames(games, network.getModel().getName(), config);

           // replayBuffer.addGames(model, games);
            List<Game> gamesOld = replayBuffer.getBuffer().getGames();
           // replayBuffer.saveState();
            //replayBuffer.loadLatestState();
            //replayBuffer.rebuildGames();

            IntStream.range(0, gamesOld.size()).forEach(i -> assertEquals(gamesOld.get(i), replayBuffer.getBuffer().getGames().get(i), "games should be the same"));
        }
    }
}
