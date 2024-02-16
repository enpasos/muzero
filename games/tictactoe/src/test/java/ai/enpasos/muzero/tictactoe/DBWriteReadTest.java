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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DBWriteReadTest {


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
    void writeAndReadTest() {
        gameBuffer.init();
       // config.setOutputDir("./build/tictactoeTest/");
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
            gameBuffer.addGames(games);

            GameBufferDTO dtoOriginal = gameBuffer.getPlanningBuffer();

            gameBuffer.setPlanningBuffer(null);
            gameBuffer.loadLatestStateIfExists();
            GameBufferDTO dtoNew = gameBuffer.getPlanningBuffer();
            assertTrue(deepEquals(dtoOriginal, dtoNew), "game buffers should be the same");

        }
    }


    @Test
    void writeAndRead2Test() {
        gameBuffer.init();
        play.deleteNetworksAndGames();
        try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
            Network network = new Network(config, model);
            Game game = config.newGame(true, true);
            game.apply(5, 6, 2, 0, 3, 4, 1, 7, 8);
            List<Game> games = List.of(game);

            modelState.setEpoch(10);
            gameBuffer.addGames(games);

            GameBufferDTO dtoOriginal = gameBuffer.getPlanningBuffer();
            gameBuffer.setPlanningBuffer(null);
            gameBuffer.loadLatestStateIfExists();
            GameBufferDTO dtoNew = gameBuffer.getPlanningBuffer();
            assertTrue(deepEquals(dtoOriginal,dtoNew), "game buffers should be the same");

        }
    }

    public boolean deepEquals(GameBufferDTO dtoOld, GameBufferDTO dtoNew) {
        // implement a deep equals
        boolean base =  dtoOld.getConfig().equals(dtoNew.getConfig())
                && dtoOld.getCounter() == dtoNew.getCounter()
                && dtoOld.getGameClassName().equals(dtoNew.getGameClassName())
                && dtoOld.getEpisodeMemory().getNumberOfEpisodes() == dtoNew.getEpisodeMemory().getNumberOfEpisodes();

        if (!base) return false;


        List<Game> oldGames = dtoOld.getEpisodeMemory().getGameList();
        List<Game> newGames = dtoNew.getEpisodeMemory().getGameList();
        for (int i = 0; i < oldGames.size(); i++) {
            if (!oldGames.get(i).deepEquals(newGames.get(i))) return false;
        }
        return true;
    }


}
