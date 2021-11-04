package ai.enpasos.muzero.pegsolitair.config.environment;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;


public class BoardTest {

    @Test
    public void testGetLegalMoves() {
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

        assertEquals(Board.neighborMap.getMap().size(), 33);

        List<Move> moves = board.getLegalMoves();

        board.applyMove(moves.get(0));

        assertEquals(board.render(),
                "   1  2  3  4  5  6  7\n" +
                        "1        O  O  O      \n" +
                        "2        O  .  O      \n" +
                        "3  O  O  O  .  O  O  O\n" +
                        "4  O  O  O  O  O  O  O\n" +
                        "5  O  O  O  O  O  O  O\n" +
                        "6        O  O  O      \n" +
                        "7        O  O  O      \n");


        moves = board.getLegalMoves();

        board.applyMove(moves.get(0));

        System.out.println(board.render());


        moves = board.getLegalMoves();

        board.applyMove(moves.get(2));

        System.out.println(board.render());


        moves = board.getLegalMoves();

        board.applyMove(moves.get(3));

        System.out.println(board.render());


        moves = board.getLegalMoves();

    }


    @Test
    public void testRandomGame() {
        Board board = new Board();
        System.out.println(board.render());
        List<Move> legalMoves = board.getLegalMoves();
        do {
            Collections.shuffle(legalMoves);
            Move move = legalMoves.get(0);
            board.applyMove(move);
            System.out.println(board.render());
            legalMoves = board.getLegalMoves();
        } while (legalMoves.size() > 0);
        System.out.println("score: " + board.getScore());
    }
}
