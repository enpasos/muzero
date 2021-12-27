package ai.enpasos.muzero.go.config.environment.scoring;


import ai.enpasos.muzero.go.config.environment.GoBoard;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BLACK_PLAYER;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WHITE_PLAYER;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class MoreScoringTest {

    // 5  X  O  O  O  .
    // 4  X  X  O  .  O
    // 3  X  X  X  O  O
    // 2  .  X  X  X  O
    // 1  .  X  X  X  X
    //    A  B  C  D  E
    @Test
    void scoreAGiven5x5GameF() {
        var board = new GoBoard(5);

        board = board.placeStone(BLACK_PLAYER, new Point(5, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(4, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(3, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(2, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(1, 1));
        board = board.placeStone(WHITE_PLAYER, new Point(1, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(1, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(1, 4));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());
        assertEquals(4.5f, result.blackWinningMargin());

    }


    // 5  X  X  O  O  .
    // 4  .  X  X  O  X
    // 3  X  X  X  O  X
    // 2  X  X  O  .  O
    // 1  X  O  O  O  .
    //    A  B  C  D  E
    @Test
    void scoreAGiven5x5GameE() {
        var board = new GoBoard(5);

        board = board.placeStone(BLACK_PLAYER, new Point(1, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(1, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(1, 4));

        board = board.placeStone(BLACK_PLAYER, new Point(2, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 5));


        board = board.placeStone(BLACK_PLAYER, new Point(3, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 5));


        board = board.placeStone(BLACK_PLAYER, new Point(4, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(5, 1));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 4));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());
        assertEquals(25.5f, -result.blackWinningMargin());
    }


    // 5  .  O  .  O  .
    // 4  O  O  O  X  X
    // 3  .  O  X  X  X
    // 2  O  O  X  X  .
    // 1  .  X  .  X  X
    //    A  B  C  D  E
    @Test
    void scoreAGiven5x5GameD() {
        var board = new GoBoard(5);

        board = board.placeStone(WHITE_PLAYER, new Point(1, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(1, 4));

        board = board.placeStone(WHITE_PLAYER, new Point(2, 1));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 5));


        board = board.placeStone(WHITE_PLAYER, new Point(3, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 5));


        board = board.placeStone(WHITE_PLAYER, new Point(4, 1));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 4));

        board = board.placeStone(BLACK_PLAYER, new Point(5, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 5));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

        assertEquals(0.5f, result.blackWinningMargin());

    }


    // 5  X  X  O  O  .
    // 4  .  X  O  O  O
    // 3  X  X  X  O  O
    // 2  .  X  O  O  .
    // 1  X  X  X  O  O
    //    A  B  C  D  E
    @Test
    void scoreAGiven5x5GameC() {
        var board = new GoBoard(5);

        board = board.placeStone(BLACK_PLAYER, new Point(1, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(1, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(1, 4));

        board = board.placeStone(BLACK_PLAYER, new Point(2, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(3, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(4, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 4));

        board = board.placeStone(BLACK_PLAYER, new Point(5, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 5));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

        assertEquals(1.5f, -result.blackWinningMargin());

    }


    // 5  O  X  .  O  X
    // 4  .  X  X  X  X
    // 3  X  X  X  .  X
    // 2  O  O  X  X  .
    // 1  O  O  .  O  .
    //    A  B  C  D  E
    @Test
    void scoreAGiven5x5GameB() {
        var board = new GoBoard(5);

        board = board.placeStone(WHITE_PLAYER, new Point(1, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(1, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(2, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(3, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 5));

        board = board.placeStone(WHITE_PLAYER, new Point(4, 1));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 4));

        board = board.placeStone(WHITE_PLAYER, new Point(5, 1));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 4));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

        assertEquals(19.5f, result.blackWinningMargin());
    }


    // bbbbb
    // b.bww
    // bbww.
    // wbww.
    // .bbww
    @Test
    void scoreAGiven5x5Game() {
        var board = new GoBoard(5);

        board = board.placeStone(BLACK_PLAYER, new Point(5, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(5, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 5));

        board = board.placeStone(WHITE_PLAYER, new Point(4, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(4, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(4, 4));

        board = board.placeStone(BLACK_PLAYER, new Point(3, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(3, 2));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(3, 4));

        board = board.placeStone(BLACK_PLAYER, new Point(2, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(2, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 4));
        board = board.placeStone(WHITE_PLAYER, new Point(2, 5));

        board = board.placeStone(BLACK_PLAYER, new Point(1, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 3));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 4));
        board = board.placeStone(BLACK_PLAYER, new Point(1, 5));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());


        assertEquals(4.5f, result.blackWinningMargin());

    }
}
