package ai.enpasos.muzero.environments.go.environment.scoring;

import ai.enpasos.muzero.environments.go.environment.GoBoard;
import ai.enpasos.muzero.environments.go.environment.Point;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.environments.go.environment.Player.BlackPlayer;
import static ai.enpasos.muzero.environments.go.environment.Player.WhitePlayer;

import static org.junit.jupiter.api.Assertions.*;

public class ScoringTest {

    // .w.ww
    // wwww.
    // bbbww
    // .bbbb
    // .b.b.
    @Test
    void scoreAGiven5x5Game() {
        var board = new GoBoard(5);

        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(BlackPlayer, new Point(1, 4));
        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(BlackPlayer, new Point(2, 4));
        board = board.placeStone(BlackPlayer, new Point(2, 5));
        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 3));

        board = board.placeStone(WhitePlayer, new Point(3, 4));
        board = board.placeStone(WhitePlayer, new Point(3, 5));
        board = board.placeStone(WhitePlayer, new Point(4, 1));
        board = board.placeStone(WhitePlayer, new Point(4, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 4));
        board = board.placeStone(WhitePlayer, new Point(5, 2));
        board = board.placeStone(WhitePlayer, new Point(5, 4));
        board = board.placeStone(WhitePlayer, new Point(5, 5));
        //println("final board configuration: \n" + board)

        var result = GameResult.apply(board, 0.5f);
        //println("result = \n" + result.toDebugString)

        // should have 9 black and white stones
        assertEquals(9, result.getNumBlackStones());
        assertEquals(9, result.getNumWhiteStones());

        // should have 4 points for black
        assertEquals(4, result.getNumBlackTerritory());

        // should have 3 points for white
        assertEquals(3, result.getNumWhiteTerritory());

        // and no dame points
        assertEquals(0, result.getNumDame());

        // Black wins by
        assertEquals(0.5, result.blackWinningMargin());

    }



    @Test
    void scoreAGiven5x5GameWithCaptures() {
        var board = new GoBoard(5);

        board = board.placeStone(BlackPlayer, new Point(1, 4));
        board = board.placeStone(BlackPlayer, new Point(2, 4));
        board = board.placeStone(BlackPlayer, new Point(3, 4));
        board = board.placeStone(BlackPlayer, new Point(4, 4));
        board = board.placeStone(BlackPlayer, new Point(5, 4));

        board = board.placeStone(WhitePlayer, new Point(1, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(3, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(5, 3));

        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(4, 2));

        board = board.placeStone(WhitePlayer, new Point(2, 5));
        board = board.placeStone(WhitePlayer, new Point(3, 5));
        board = board.placeStone(WhitePlayer, new Point(4, 5));

        board = board.placeStone(BlackPlayer, new Point(5, 5)); // fills second to last liberty
        board = board.placeStone(WhitePlayer, new Point(1, 5)); // captures 6 black stones
        board = board.placeStone(BlackPlayer, new Point(5, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 4)); // plays in white result
        board = board.placeStone(BlackPlayer, new Point(1, 2)); // secures 5 points black result
        //println("final board configuration: \n" + board)

        var result = GameResult.apply(board, 0.5f);

        // Expected stones on the board
        assertEquals(10, result.getNumWhiteStones());
        assertEquals(5, result.getNumBlackStones());

        // should have 4 points for black
            assertEquals(5, result.getNumBlackTerritory());
        // should have 3 points for white
            assertEquals(5, result.getNumWhiteTerritory());
        // and no dame points
        assertEquals(0, result.getNumDame());
        // White wins by
        assertEquals(11.5, -result.blackWinningMargin());

    }


    @Test
    void scoreAGiven5x5GameWithCapturesAndDame() {
        var board = new GoBoard(5);

        board = board.placeStone(BlackPlayer, new Point(1, 5));
        board = board.placeStone(BlackPlayer, new Point(2, 5));
        board = board.placeStone(BlackPlayer, new Point(3, 5));

        board = board.placeStone(WhitePlayer, new Point(1, 4));
        board = board.placeStone(WhitePlayer, new Point(2, 4));
        board = board.placeStone(WhitePlayer, new Point(3, 4));
        board = board.placeStone(WhitePlayer, new Point(4, 4));
        board = board.placeStone(WhitePlayer, new Point(5, 4));

        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(4, 2));
        board = board.placeStone(BlackPlayer, new Point(5, 2));

        board = board.placeStone(WhitePlayer, new Point(4, 5)); // capture 3 black stones and make life
        //println("final board configuration: \n" + board)

        var result = GameResult.apply(board, 0.5f);

        // Expected stones on the board
            assertEquals(6, result.getNumWhiteStones());
            assertEquals(5, result.getNumBlackStones());
        // should have 4 points for black
            assertEquals(5, result.getNumBlackTerritory());
        // should have 3 points for white
            assertEquals(4, result.getNumWhiteTerritory());
        // and several dame points
        assertEquals(5, result.getNumDame());
        // White wins by
        assertEquals(3.5, -result.blackWinningMargin());

    }

    /*
     * 5 .O.XO
     * 4 OOOOO
     * 3 XXXXO
     * 2 .XXXX
     * 1 XX.XX
     *   ABCDE
     */
    @Test
    void scoreAGiven5x5GameWithNarrowWhiteVictory() {
            var board = new GoBoard(5);
        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(BlackPlayer, new Point(1, 4));
        board = board.placeStone(BlackPlayer, new Point(1, 5));
        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(BlackPlayer, new Point(2, 4));
        board = board.placeStone(BlackPlayer, new Point(2, 5));
        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 3));
        board = board.placeStone(BlackPlayer, new Point(3, 4));
        board = board.placeStone(BlackPlayer, new Point(5, 4));

        board = board.placeStone(WhitePlayer, new Point(5, 2));
        board = board.placeStone(WhitePlayer, new Point(5, 5));
        board = board.placeStone(WhitePlayer, new Point(4, 1));
        board = board.placeStone(WhitePlayer, new Point(4, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 4));
        board = board.placeStone(WhitePlayer, new Point(4, 5));
        board = board.placeStone(WhitePlayer, new Point(3, 5));
        //println("final board configuration: \n" + board)

        var result = GameResult.apply(board, 6.5f);
        //println("result = \n" + result.toDebugString)

        // should have expected black and white stones
            assertEquals(12, result.getNumBlackStones());
            assertEquals(8, result.getNumWhiteStones());

        // should have expected black territory
            assertEquals(2, result.getNumBlackTerritory());

        // should have expected white territory
            assertEquals(3, result.getNumWhiteTerritory());


        // should have expected black points
            assertEquals(14, result.blackPoints());

        // should have expected white points
            assertEquals(12, result.whitePoints());


        // and no dame points
            assertEquals(0, result.getNumDame());

        // Black wins by
            assertEquals(-4.5, result.blackWinningMargin());

    }

    /*
     * 5 .O.XX
     * 4 OOOOO
     * 3 .....
     * 2 .XXXX
     * 1 .X.X.
     *   ABCDE
     */
    @Test
    void scoreAGiven5x5GameWithLotsOfDames() {

        var board = new GoBoard(5);
        board = board.placeStone(WhitePlayer, new Point(5, 2));
        board = board.placeStone(BlackPlayer, new Point(5, 4));
        board = board.placeStone(BlackPlayer, new Point(5, 5));
        board = board.placeStone(WhitePlayer, new Point(4, 1));
        board = board.placeStone(WhitePlayer, new Point(4, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 4));
        board = board.placeStone(WhitePlayer, new Point(4, 5));
        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(BlackPlayer, new Point(2, 4));
        board = board.placeStone(BlackPlayer, new Point(2, 5));
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(BlackPlayer, new Point(1, 4));

        var result = GameResult.apply(board, 0.5f);
        //println("result = \n" + result.toDebugString)

        // should have expected black and white stones
            assertEquals(6, result.getNumBlackStones());
            assertEquals(6, result.getNumWhiteStones());

        // should have expected black territory
            assertEquals(2, result.getNumBlackTerritory());

        // should have expected white territory
            assertEquals(4, result.getNumWhiteTerritory());


        // should have expected black points
            assertEquals(8,result.blackPoints());

        // should have expected white points
            assertEquals(12, result.whitePoints());


        // and no dame points
            assertEquals(7, result.getNumDame());

        // Black wins by
            assertEquals(-4.5, result.blackWinningMargin());

    }


    @Test
    void scoreAGiven5x5GameWithBigBlackVictory() {

        var board = new GoBoard(5);

        // first two rows of white stones that will be captured
        for (int i = 1; i <= 5; i++) {
            board = board.placeStone(WhitePlayer, new Point(1, i));
            board = board.placeStone(WhitePlayer, new Point(2, i));
        }
        // now capture them
        for (int i = 1; i <= 5; i++) {
            board = board.placeStone(BlackPlayer, new Point(3, i));
        }
        // create white group with 2 eyes
        board = board.placeStone(WhitePlayer, new Point(4, 1));
        board = board.placeStone(WhitePlayer, new Point(4, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 4));
        board = board.placeStone(WhitePlayer, new Point(5, 2));
        board = board.placeStone(WhitePlayer, new Point(5, 4));
        //println("final board configuration: \n" + board)

        var result = GameResult.apply(board, 0.5f);
        //println("result = \n" + result.toDebugString)

        // Expected stones on the board
            assertEquals(6, result.getNumWhiteStones());
            assertEquals(5, result.getNumBlackStones());

        // should have 4 points for black
            assertEquals(10, result.getNumBlackTerritory());

        // should have 3 points for white
            assertEquals(2, result.getNumWhiteTerritory());

        // and dame points
            assertEquals(2, result.getNumDame());

        // Black wins by
            assertEquals(16.5, result.blackWinningMargin());

    }
}
