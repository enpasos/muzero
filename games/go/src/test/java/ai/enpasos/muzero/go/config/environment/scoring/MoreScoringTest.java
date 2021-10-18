package ai.enpasos.muzero.go.config.environment.scoring;


import ai.enpasos.muzero.go.config.environment.GoBoard;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import ai.enpasos.muzero.go.config.environment.scoring.GameResult;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BlackPlayer;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WhitePlayer;

@Slf4j
public class MoreScoringTest {

    // 5  X  O  O  O  .
    // 4  X  X  O  .  O
    // 3  X  X  X  O  O
    // 2  .  X  X  X  O
    // 1  .  X  X  X  X
    //    A  B  C  D  E
    @Test
    void scoreAGiven5x5GameF() {
        var board = new GoBoard(5);

        board = board.placeStone(BlackPlayer, new Point(5, 2));
        board = board.placeStone(BlackPlayer, new Point(5, 3));
        board = board.placeStone(BlackPlayer, new Point(5, 4));
        board = board.placeStone(BlackPlayer, new Point(5, 5));

        board = board.placeStone(BlackPlayer, new Point(4, 2));
        board = board.placeStone(BlackPlayer, new Point(4, 3));
        board = board.placeStone(BlackPlayer, new Point(4, 4));
        board = board.placeStone(WhitePlayer, new Point(4, 5));

        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 3));
        board = board.placeStone(WhitePlayer, new Point(3, 4));
        board = board.placeStone(WhitePlayer, new Point(3, 5));

        board = board.placeStone(BlackPlayer, new Point(2, 1));
        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(WhitePlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 5));

        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(WhitePlayer, new Point(1, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 3));
        board = board.placeStone(WhitePlayer, new Point(1, 4));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

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

        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 3));
        board = board.placeStone(WhitePlayer, new Point(1, 4));

        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 4));
        board = board.placeStone(BlackPlayer, new Point(2, 5));


        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 3));
        board = board.placeStone(WhitePlayer, new Point(3, 4));
        board = board.placeStone(BlackPlayer, new Point(3, 5));


        board = board.placeStone(BlackPlayer, new Point(4, 1));
        board = board.placeStone(BlackPlayer, new Point(4, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 5));

        board = board.placeStone(BlackPlayer, new Point(5, 1));
        board = board.placeStone(WhitePlayer, new Point(5, 2));
        board = board.placeStone(WhitePlayer, new Point(5, 3));
        board = board.placeStone(WhitePlayer, new Point(5, 4));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

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

        board = board.placeStone(WhitePlayer, new Point(1, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 4));

        board = board.placeStone(WhitePlayer, new Point(2, 1));
        board = board.placeStone(WhitePlayer, new Point(2, 2));
        board = board.placeStone(WhitePlayer, new Point(2, 3));
        board = board.placeStone(BlackPlayer, new Point(2, 4));
        board = board.placeStone(BlackPlayer, new Point(2, 5));


        board = board.placeStone(WhitePlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 3));
        board = board.placeStone(BlackPlayer, new Point(3, 4));
        board = board.placeStone(BlackPlayer, new Point(3, 5));


        board = board.placeStone(WhitePlayer, new Point(4, 1));
        board = board.placeStone(WhitePlayer, new Point(4, 2));
        board = board.placeStone(BlackPlayer, new Point(4, 3));
        board = board.placeStone(BlackPlayer, new Point(4, 4));

        board = board.placeStone(BlackPlayer, new Point(5, 2));
        board = board.placeStone(BlackPlayer, new Point(5, 4));
        board = board.placeStone(BlackPlayer, new Point(5, 5));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

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

        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 3));
        board = board.placeStone(WhitePlayer, new Point(1, 4));

        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(WhitePlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 4));
        board = board.placeStone(WhitePlayer, new Point(2, 5));

        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 3));
        board = board.placeStone(WhitePlayer, new Point(3, 4));
        board = board.placeStone(WhitePlayer, new Point(3, 5));

        board = board.placeStone(BlackPlayer, new Point(4, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 4));

        board = board.placeStone(BlackPlayer, new Point(5, 1));
        board = board.placeStone(BlackPlayer, new Point(5, 2));
        board = board.placeStone(BlackPlayer, new Point(5, 3));
        board = board.placeStone(WhitePlayer, new Point(5, 4));
        board = board.placeStone(WhitePlayer, new Point(5, 5));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

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

        board = board.placeStone(WhitePlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 4));
        board = board.placeStone(BlackPlayer, new Point(1, 5));

        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(BlackPlayer, new Point(2, 4));
        board = board.placeStone(BlackPlayer, new Point(2, 5));

        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 3));
        board = board.placeStone(BlackPlayer, new Point(3, 5));

        board = board.placeStone(WhitePlayer, new Point(4, 1));
        board = board.placeStone(WhitePlayer, new Point(4, 2));
        board = board.placeStone(BlackPlayer, new Point(4, 3));
        board = board.placeStone(BlackPlayer, new Point(4, 4));

        board = board.placeStone(WhitePlayer, new Point(5, 1));
        board = board.placeStone(WhitePlayer, new Point(5, 2));
        board = board.placeStone(WhitePlayer, new Point(5, 4));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

    }



    // bbbbb
    // b.bww
    // bbww.
    // wbww.
    // .bbww
    @Test
    void scoreAGiven5x5Game() {
        var board = new GoBoard(5);

        board = board.placeStone(BlackPlayer, new Point(5, 2));
        board = board.placeStone(BlackPlayer, new Point(5, 3));
        board = board.placeStone(WhitePlayer, new Point(5, 4));
        board = board.placeStone(WhitePlayer, new Point(5, 5));

        board = board.placeStone(WhitePlayer, new Point(4, 1));
        board = board.placeStone(BlackPlayer, new Point(4, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 4));

        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(WhitePlayer, new Point(3, 3));
        board = board.placeStone(WhitePlayer, new Point(3, 4));

        board = board.placeStone(BlackPlayer, new Point(2, 1));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 4));
        board = board.placeStone(WhitePlayer, new Point(2, 5));

        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(BlackPlayer, new Point(1, 3));
        board = board.placeStone(BlackPlayer, new Point(1, 4));
        board = board.placeStone(BlackPlayer, new Point(1, 5));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board, 0.5f);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

    }
}
