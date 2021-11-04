package ai.enpasos.muzero.pegsolitair.config.environment;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;


public class BoardTest {

    @Test
    public void testGetLegalMoves() {
        Board board = new Board();
        assertEquals(Board.neighborMap.getMap().size(), 33);

        List<Move> moves = board.getLegalMoves();
    }
}
