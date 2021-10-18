package ai.enpasos.muzero.go;

import ai.enpasos.muzero.go.config.environment.GameState;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import ai.enpasos.muzero.go.config.environment.basics.move.Play;
import org.testng.annotations.Test;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BlackPlayer;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WhitePlayer;
import static org.testng.AssertJUnit.assertTrue;

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
