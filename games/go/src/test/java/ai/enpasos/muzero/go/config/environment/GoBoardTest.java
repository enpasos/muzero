package ai.enpasos.muzero.go.config.environment;

import ai.enpasos.muzero.go.config.environment.basics.Point;
import org.testng.annotations.Test;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BlackPlayer;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WhitePlayer;
import static org.testng.AssertJUnit.*;


public class GoBoardTest {


    @Test
    void capturingAStoneOnANewBoard() {
        var board = new GoBoard(9);

        // should place and confirm a black stone
        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 2));
        assertSame(board.getPlayer(new Point(2, 2)).get(), BlackPlayer);

        // if black's liberties go down to two, the stone should still be there
        board = board.placeStone(WhitePlayer, new Point(2, 1));
        assertSame(board.getPlayer(new Point(2, 2)).get(), BlackPlayer);

        // if black's liberties go down to one, the stone should still be there
        board = board.placeStone(WhitePlayer, new Point(2, 3));
        assertSame(board.getPlayer(new Point(2, 2)).get(), BlackPlayer);

        //   finally, if all liberties are taken, the stone should be gone
        board = board.placeStone(WhitePlayer, new Point(3, 2));
        assertTrue(board.getPlayer(new Point(2, 2)).isEmpty());

    }

    @Test
    void capturingTwoStonesOnANewBoard() {
        var board = new GoBoard(9);

        // should place and confirm two black stones
        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(1, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 3));

        assertSame(board.getPlayer(new Point(2, 2)).get(), BlackPlayer);
        assertSame(board.getPlayer(new Point(2, 3)).get(), BlackPlayer);

        // if black's liberties go down to two, the stone should still be there
        board = board.placeStone(WhitePlayer, new Point(3, 2));
        board = board.placeStone(WhitePlayer, new Point(3, 3));
        assertSame(board.getPlayer(new Point(2, 2)).get(), BlackPlayer);
        assertSame(board.getPlayer(new Point(2, 3)).get(), BlackPlayer);

        // finally, if all liberties are taken, the stone should be gone
        board = board.placeStone(WhitePlayer, new Point(2, 1));
        board = board.placeStone(WhitePlayer, new Point(2, 4));
        assertTrue(board.getPlayer(new Point(2, 2)).isEmpty());
        assertTrue(board.getPlayer(new Point(2, 3)).isEmpty());

    }


    @Test
    void ifYouCaptureAStoneItIsNotSuicide() {
        var board = new GoBoard(9);
        // should regain liberties by capturing
        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(1, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 1));
        board = board.placeStone(WhitePlayer, new Point(1, 2));
        assertTrue(board.getPlayer(new Point(1, 1)).isEmpty());
        assertSame(board.getPlayer(new Point(2, 1)).get(), WhitePlayer);
        assertSame(board.getPlayer(new Point(1, 2)).get(), WhitePlayer);
    }

    @Test
    void fillingYourOwnEyeIsProhibited() {
        var board = createBoardWithEyes();
        // Filling non-eye allowed
        assertFalse(board.doesMoveFillEye(WhitePlayer, new Point(2, 1)));
        assertFalse(board.doesMoveFillEye(WhitePlayer, new Point(1, 1)));
        assertFalse(board.doesMoveFillEye(WhitePlayer, new Point(1, 5)));
        assertFalse(board.doesMoveFillEye(WhitePlayer, new Point(2, 4)));
        assertFalse(board.doesMoveFillEye(BlackPlayer, new Point(2, 1)));
        assertFalse(board.doesMoveFillEye(BlackPlayer, new Point(5, 1)));
        assertFalse(board.doesMoveFillEye(WhitePlayer, new Point(5, 5)));

        // Filling eye not allowed
        assertTrue(board.doesMoveFillEye(WhitePlayer, new Point(5, 1)));
        assertTrue(board.doesMoveFillEye(WhitePlayer, new Point(3, 3)));
    }


    /**
     * 1 .....
     * 2 .oo.o
     * 3 xo.o.
     * 4 ooooo
     * 5 .o...
     */
    GoBoard createBoardWithEyes() {
        var board = new GoBoard(5);
        board = board.placeStone(WhitePlayer, new Point(4, 1));
        board = board.placeStone(WhitePlayer, new Point(4, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 4));
        board = board.placeStone(WhitePlayer, new Point(4, 5));
        board = board.placeStone(WhitePlayer, new Point(5, 2));
        board = board.placeStone(WhitePlayer, new Point(3, 2));
        board = board.placeStone(WhitePlayer, new Point(3, 4));
        board = board.placeStone(WhitePlayer, new Point(2, 2));
        board = board.placeStone(WhitePlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 5));
        board = board.placeStone(BlackPlayer, new Point(3, 1));
        return board;
    }


    /**
     * Sometimes it's hard to tell if its one of the 2 remaining eyes
     */
    @Test
    void fillingYourOwnEyeShouldBeProhibitedInTheseEdgeCases() {
        var board = createBoardWithEdgeCaseEyes();
        //println(board)
        //Filling non-eye allowed
        assertFalse(board.doesMoveFillEye(BlackPlayer, new Point(2, 2)));
        assertFalse(board.doesMoveFillEye(WhitePlayer, new Point(7, 3)));
        assertFalse(board.doesMoveFillEye(BlackPlayer, new Point(1, 1)));

        // Filling eye not allowed
        assertTrue(board.doesMoveFillEye(WhitePlayer, new Point(4, 6)));
        assertTrue(board.doesMoveFillEye(WhitePlayer, new Point(8, 2)));
        assertTrue(board.doesMoveFillEye(WhitePlayer, new Point(9, 1)));
        assertTrue(board.doesMoveFillEye(BlackPlayer, new Point(7, 9)));
        assertTrue(board.doesMoveFillEye(BlackPlayer, new Point(6, 8)));
        assertTrue(board.doesMoveFillEye(WhitePlayer, new Point(3, 5)));

    }


    /**
     * There are eyes in this board that may be hard to recognize using a strict approach.
     * 1 .X.......  // no eye
     * 2 X.X.OO...  // no eyes
     * 3 ...O.OO..  // white eye 3,5
     * 4 ...OO.O..  // white eye 4,6
     * 5 ....OOXXX
     * 6 ......X.X  // black eye 6,8
     * 7 OO.....X.  // black eye 6,9
     * 8 O.O....XX  // white eye 8,2
     * 9 .OO......  // white eye 9,1
     */
    GoBoard createBoardWithEdgeCaseEyes() {
        var board = new GoBoard(9);
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 1));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 5));
        board = board.placeStone(WhitePlayer, new Point(2, 6));
        board = board.placeStone(WhitePlayer, new Point(3, 4));
        board = board.placeStone(WhitePlayer, new Point(3, 6));
        board = board.placeStone(WhitePlayer, new Point(3, 7));
        board = board.placeStone(WhitePlayer, new Point(4, 4));
        board = board.placeStone(WhitePlayer, new Point(4, 5));
        board = board.placeStone(WhitePlayer, new Point(4, 7));
        board = board.placeStone(WhitePlayer, new Point(5, 5));
        board = board.placeStone(WhitePlayer, new Point(5, 6));
        board = board.placeStone(BlackPlayer, new Point(5, 7));
        board = board.placeStone(BlackPlayer, new Point(5, 8));
        board = board.placeStone(BlackPlayer, new Point(5, 9));
        board = board.placeStone(BlackPlayer, new Point(6, 7));
        board = board.placeStone(BlackPlayer, new Point(6, 9));
        board = board.placeStone(WhitePlayer, new Point(7, 1));
        board = board.placeStone(WhitePlayer, new Point(7, 2));
        board = board.placeStone(BlackPlayer, new Point(7, 8));
        board = board.placeStone(WhitePlayer, new Point(8, 1));
        board = board.placeStone(WhitePlayer, new Point(8, 3));
        board = board.placeStone(BlackPlayer, new Point(8, 8));
        board = board.placeStone(BlackPlayer, new Point(8, 9));
        board = board.placeStone(WhitePlayer, new Point(9, 2));
        board = board.placeStone(WhitePlayer, new Point(9, 3));
        return board;
    }

    @Test
    void testRemovingLiberties() {
        // a stone with four liberties should end up with three if an opponent stone is added
        var board = new GoBoard(5);
        board = board.placeStone(BlackPlayer, new Point(3, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 2));
        var whiteString = board.getGoString(new Point(2, 2)).get();
        assertEquals(4, whiteString.numLiberties());

     //   board = board.placeStone(BlackPlayer, new Point(3, 2));

    }

    @Test
    void emptyTriangleTest() {
        // an empty triangle in the corner with one white stone should have 3 liberties
        // x x
        // x o
        var board = new GoBoard(5);
        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(WhitePlayer, new Point(2, 1));

        GoString blackString = board.getGoString(new Point(1, 1)).get();

        assertEquals(3, blackString.numLiberties());
        assertTrue(blackString.getLiberties().contains(new Point(3, 2)));
        assertTrue(blackString.getLiberties().contains(new Point(2, 3)));
        assertTrue(blackString.getLiberties().contains(new Point(1, 3)));
        //println(board)

    }

    @Test
    void testSelfCapture() {
        // ooo..
        // x.xo.
        // black can't take it's own last liberty"
        var board = new GoBoard(5);
        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 1));
        board = board.placeStone(WhitePlayer, new Point(2, 2));
        board = board.placeStone(WhitePlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(1, 4));
        //println(board)
        assertTrue(board.isSelfCapture(BlackPlayer, new Point(1, 2)));


        // o.o..
        // x.xo.
        // but if we remove one white stone, the move becomes legal
        board = new GoBoard(5);
        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 1));
        board = board.placeStone(WhitePlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(1, 4));
        //println(board)
        assertFalse(board.isSelfCapture(BlackPlayer, new Point(1, 2)));


        // xx...
        // oox..
        // x.o..
        // if we capture a stone in the process, it's not self-atari
        board = new GoBoard(5);
        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(WhitePlayer, new Point(2, 1));
        board = board.placeStone(WhitePlayer, new Point(2, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 3));
        //println(board)
        assertFalse(board.isSelfCapture(BlackPlayer, new Point(1, 2)));


        // xx...
        // o.x..
        // xxx..
        // Should not be able refill eye after capture
        board = new GoBoard(5);
        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(WhitePlayer, new Point(2, 1));
        board = board.placeStone(WhitePlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(1, 3));
        //println(board)
        board = board.placeStone(BlackPlayer, new Point(1, 2)); // captures 2 stones
        assertSame(board.getPlayer(new Point(1, 2)).get(), BlackPlayer);
        //println("just played Black at 1, 2 (capturing 2 white stones)\n" + board)
        board = board.placeStone(WhitePlayer, new Point(2, 1)); // refill first of 2 spaces in eye
        //println("just played White at 2, 1\n" + board);
        assertSame(board.getPlayer(new Point(1, 2)).get(), BlackPlayer);

        assertTrue(board.isSelfCapture(WhitePlayer, new Point(2, 2)));


        // xx...
        // o.x..
        // xxo..
        // OK to refill eye after capture if doing so captures opponent stones
        board = new GoBoard(5);
        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(WhitePlayer, new Point(2, 1));
        board = board.placeStone(WhitePlayer, new Point(2, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 3));
        //println(board)
        board = board.placeStone(BlackPlayer, new Point(1, 2));// captures 2 stones
        assert (board.getPlayer(new Point(1, 2)).get() == BlackPlayer);
        //println("just played Black at 1, 2 (capturing 2 white stones)\n" + board)
        board = board.placeStone(WhitePlayer, new Point(2, 1)); // refill first of 2 spaces in eye
        //println("just played White at 2, 1\n" + board)
        assert (board.getPlayer(new Point(1, 2)).get() == BlackPlayer);

        //println("White playing at 2,2 is OK because it captures 2 plack stones in doing so")
        assert (!board.isSelfCapture(WhitePlayer, new Point(2, 2)));
    //    board = board.placeStone(WhitePlayer, new Point(2, 2));
        //println("just played White at 2, 2\n" + board)

    }

}
