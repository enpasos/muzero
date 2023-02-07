package ai.enpasos.muzero.go;

import ai.enpasos.muzero.go.config.environment.GameState;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import ai.enpasos.muzero.go.config.environment.basics.move.Play;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BLACK_PLAYER;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WHITE_PLAYER;
import static org.junit.jupiter.api.Assertions.assertSame;



class GameStateTest {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void startingANew19x19Game() {
        var start = GameState.newGame(19);
        // should apply moves
        var nextState = start.applyMove(new Play(new Point(16, 16)));
        assertSame(start, nextState.getPreviousState());
        assertSame(BLACK_PLAYER, nextState.getBoard().getPlayer(new Point(16, 16)).get());
        assertSame(WHITE_PLAYER, nextState.getNextPlayer());
    }


}
