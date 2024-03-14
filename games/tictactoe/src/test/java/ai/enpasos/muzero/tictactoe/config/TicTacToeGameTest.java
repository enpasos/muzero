package ai.enpasos.muzero.tictactoe.config;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class TicTacToeGameTest {

    @Autowired
    MuZeroConfig config;

    @Test
    void test() {
        check(   1, 5, 0, 4, 7, 2, 8, 6);

    }


    private void check(  int... actions) {
        config.setNumObservationLayers(3);
        Game game = config.newGame(true,true);
        for (int i = 0; i < actions.length; i++) {
            int a = actions[i];
            Objects.requireNonNull(game).apply(config.newAction(a));
        }
        TicTacToeGame ticTacToeGame = (TicTacToeGame) game;
        assertEquals(ticTacToeGame.getObservationModelInputOld(3), ticTacToeGame.getObservationModelInput(3));
    }
}
