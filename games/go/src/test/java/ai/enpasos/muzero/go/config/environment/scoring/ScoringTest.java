package ai.enpasos.muzero.go.config.environment.scoring;

import ai.enpasos.muzero.go.config.environment.GoBoard;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BLACK_PLAYER;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WHITE_PLAYER;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
public class ScoringTest {

    // ------------------
    // 5  .  X  .  X  .
    // 4  .  X  X  X  X
    // 3  X  X  X  O  O
    // 2  O  O  O  O  .
    // 1  .  O  .  O  O
    //    A  B  C  D  E
    //------------------
    @Test
    void scoreAGiven5x5Game() {
        var board = new GoBoard(5);

        board = board.placeStone(BLACK_PLAYER, new Point(1, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 5));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 3));

        board = board.placeStone(WHITE_PLAYER, new Point(3, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 5));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 1));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 5));
        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

        // should have 9 black and white stones
        assertEquals(result.getNumBlackStones(), 9);
        assertEquals(result.getNumWhiteStones(), 9);

        // should have 4 points for black
        assertEquals(result.getNumBlackTerritory(), 4);

        // should have 3 points for white
        assertEquals(result.getNumWhiteTerritory(), 3);

        // and no dame points
        assertEquals(result.getNumDame(), 0);

        // Black wins by
        assertEquals(result.blackWinningMargin(), 0.5);

    }


    // ------------------
    // 5  .  X  .  X  .
    // 4  .  X  X  X  X
    // 3  X  X  X  O  O
    // 2  O  O  O  O  .
    // 1  .  O  .  O  O
    //    A  B  C  D  E
    //------------------
    @Test
    void scoreAGiven5x5GameWithCaptures() {
        var board = new GoBoard(5);

        board = board.placeStone(BLACK_PLAYER, new Point(1, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 4));

        board = board.placeStone(WHITE_PLAYER, new Point(1, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 3));

        board = board.placeStone(BLACK_PLAYER, new Point(2, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 2));

        board = board.placeStone(WHITE_PLAYER, new Point(2, 5));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 5));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(5, 5)); // fills second to last liberty
        board = board.placeStone(WHITE_PLAYER, new Point(1, 5)); // captures 6 black stones
        board = board.placeStone(BLACK_PLAYER, new Point(5, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 4)); // plays in white result
        board = board.placeStone(BLACK_PLAYER, new Point(1, 2)); // secures 5 points black result
        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

        // Expected stones on the board
        assertEquals(result.getNumWhiteStones(), 10);
        assertEquals(result.getNumBlackStones(), 5);


        assertEquals(result.getNumBlackTerritory(), 5);

        assertEquals(result.getNumWhiteTerritory(), 5);
        // and no dame points
        assertEquals(result.getNumDame(), 0);
        // White wins by
        assertEquals(-result.blackWinningMargin(), 5.5);

    }


    @Test
    void scoreAGiven5x5GameWithCapturesAndDame() {
        var board = new GoBoard(5);

        board = board.placeStone(BLACK_PLAYER, new Point(1, 5));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 5));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 5));

        board = board.placeStone(WHITE_PLAYER, new Point(1, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 4));

        board = board.placeStone(BLACK_PLAYER, new Point(1, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 2));

        board = board.placeStone(WHITE_PLAYER, new Point(4, 5)); // capture 3 black stones and make life
        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

        // Expected stones on the board
        assertEquals(result.getNumWhiteStones(), 6);
        assertEquals(result.getNumBlackStones(), 5);

        assertEquals(result.getNumBlackTerritory(), 5);

        assertEquals(result.getNumWhiteTerritory(), 4);
        // and several dame points
        assertEquals(result.getNumDame(), 5);
        // White wins by
        assertEquals(-result.blackWinningMargin(), 0.5);

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
        board = board.placeStone(BLACK_PLAYER, new Point(1, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 5));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 5));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 4));

        board = board.placeStone(WHITE_PLAYER, new Point(5, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 5));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 1));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 5));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 5));
        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 7.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

        // should have expected black and white stones
        assertEquals(result.getNumBlackStones(), 12);
        assertEquals(result.getNumWhiteStones(), 8);

        // should have expected black territory
        assertEquals(result.getNumBlackTerritory(), 2);

        // should have expected white territory
        assertEquals(result.getNumWhiteTerritory(), 3);


        // should have expected black points
        assertEquals(result.blackPoints(), 14);

        // should have expected white points
        assertEquals(result.whitePoints(), 11);


        // and no dame points
        assertEquals(result.getNumDame(), 0);

        // Black wins by
        assertEquals(result.blackWinningMargin(), -4.5);

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
        board = board.placeStone(WHITE_PLAYER, new Point(5, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 5));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 1));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 5));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 5));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 4));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

        // should have expected black and white stones
        assertEquals(result.getNumBlackStones(), 6);
        assertEquals(result.getNumWhiteStones(), 6);

        // should have expected black territory
        assertEquals(result.getNumBlackTerritory(), 2);

        // should have expected white territory
        assertEquals(result.getNumWhiteTerritory(), 4);


        // should have expected black points
        assertEquals(result.blackPoints(), 8);

        // should have expected white points
        assertEquals(result.whitePoints(), 10);


        // and no dame points
        assertEquals(result.getNumDame(), 7);

        // Black wins by
        assertEquals(result.blackWinningMargin(), -2.5);

    }


    @Test
    void scoreAGiven5x5GameWithBigBlackVictory() {

        var board = new GoBoard(5);

        // first two rows of white stones that will be captured
        for (int i = 1; i <= 5; i++) {
            board = board.placeStone(WHITE_PLAYER, new Point(1, i));
            board = board.placeStone(WHITE_PLAYER, new Point(2, i));
        }
        // now capture them
        for (int i = 1; i <= 5; i++) {
            board = board.placeStone(BLACK_PLAYER, new Point(3, i));
        }
        // create white group with 2 eyes
        board = board.placeStone(WHITE_PLAYER, new Point(4, 1));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 4));
        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

        // Expected stones on the board
        assertEquals(result.getNumWhiteStones(), 6);
        assertEquals(result.getNumBlackStones(), 5);

        // should have 4 points for black
        assertEquals(result.getNumBlackTerritory(), 10);

        // should have 3 points for white
        assertEquals(result.getNumWhiteTerritory(), 2);

        // and dame points
        assertEquals(result.getNumDame(), 2);

        // Black wins by
        assertEquals(result.blackWinningMargin(), 6.5);

    }
}
