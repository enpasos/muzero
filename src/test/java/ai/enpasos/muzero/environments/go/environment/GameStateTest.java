package ai.enpasos.muzero.environments.go.environment;

import ai.enpasos.muzero.environments.go.environment.basics.Point;
import ai.enpasos.muzero.environments.go.environment.basics.move.Play;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.environments.go.environment.basics.Player.BlackPlayer;
import static ai.enpasos.muzero.environments.go.environment.basics.Player.WhitePlayer;
import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void startingANew19x19Game() {
        var start = GameState.newGame(19);
        // should apply moves
        var nextState = start.applyMove(new Play(new Point(16, 16)));
        assertTrue(start == nextState.getPreviousState().get());
        assertTrue(nextState.getBoard().getPlayer(new Point(16, 16)).get() == BlackPlayer);
        assertTrue(nextState.getNextPlayer() == WhitePlayer);
    }


}
