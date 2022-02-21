package ai.enpasos.muzero.pegsolitair.config.environment;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class BoardTest {

    @Test
    void testGetLegalMoves() {
        Board board = new Board();

        assertEquals(board.render(),
            "   1  2  3  4  5  6  7\n" +
                "1        O  O  O      \n" +
                "2        O  O  O      \n" +
                "3  O  O  O  O  O  O  O\n" +
                "4  O  O  O  .  O  O  O\n" +
                "5  O  O  O  O  O  O  O\n" +
                "6        O  O  O      \n" +
                "7        O  O  O      \n");

        assertEquals(33, Board.neighborMap.getMap().size());

        List<Jump> jumps = board.getLegalJumps();

        board.applyJump(jumps.get(0));

        assertEquals(board.render(),
            "   1  2  3  4  5  6  7\n" +
                "1        O  O  O      \n" +
                "2        O  .  O      \n" +
                "3  O  O  O  .  O  O  O\n" +
                "4  O  O  O  O  O  O  O\n" +
                "5  O  O  O  O  O  O  O\n" +
                "6        O  O  O      \n" +
                "7        O  O  O      \n");


        jumps = board.getLegalJumps();

        board.applyJump(jumps.get(0));

        System.out.println(board.render());


        jumps = board.getLegalJumps();

        board.applyJump(jumps.get(2));

        System.out.println(board.render());


        jumps = board.getLegalJumps();

        board.applyJump(jumps.get(3));

        System.out.println(board.render());


        //   jumps = board.getLegalJumps();

    }


    @Test
    void testRandomGame() {
        Board board = new Board();
        System.out.println(board.render());
        List<Jump> legalMoves = board.getLegalJumps();
        do {
            Collections.shuffle(legalMoves);
            Jump move = legalMoves.get(0);
            board.applyJump(move);
            System.out.println(board.render());
            legalMoves = board.getLegalJumps();
        } while (legalMoves.size() > 0);
        System.out.println("score: " + board.getScore());
        assertTrue(board.holesOnTheBoard.size() > 10);
    }


    @Test
    void testIsOnePegInTheMiddle() {

        Board board = new Board();
        assertFalse(board.isOnePegInTheMiddle());
        List<Jump> jumps = board.getLegalJumps();

        board.applyJump(jumps.get(0));
        assertTrue(board.isOnePegInTheMiddle());
    }
}
