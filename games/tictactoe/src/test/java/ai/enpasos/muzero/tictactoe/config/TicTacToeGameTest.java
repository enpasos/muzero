package ai.enpasos.muzero.tictactoe.config;

import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
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
    void getObservationModelInput() {
        check(  5, 5, 0, 4, 7, 2, 8, 6);
    }

    private void check(int numObservationLayers,  int... actions) {
        config.setNumObservationLayers(numObservationLayers);
        Game game = config.newGame(true,true);
        for (int i = 0; i < actions.length; i++) {
            int a = actions[i];
            Objects.requireNonNull(game).apply(config.newAction(a));
        }
        ObservationModelInput omi = game.getObservationModelInput();
        assertEquals(3, omi.getShape().length);
        assertEquals(numObservationLayers, omi.getShape()[0]);
    }
}
