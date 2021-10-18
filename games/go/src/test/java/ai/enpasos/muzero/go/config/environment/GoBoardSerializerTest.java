package ai.enpasos.muzero.go.config.environment;

import ai.enpasos.muzero.go.config.environment.GoBoard;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import org.testng.annotations.Test;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BlackPlayer;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WhitePlayer;
import static org.testng.AssertJUnit.assertEquals;

class GoBoardSerializerTest {

    @Test
    void serializeABoard1() {
                var board = new GoBoard(9);

            board = board.placeStone(BlackPlayer, new Point(2, 2));
            board = board.placeStone(WhitePlayer, new Point(3, 3));
            board = board.placeStone(BlackPlayer, new Point(2, 3));
            board = board.placeStone(WhitePlayer, new Point(5, 4));

            assertEquals("------------------------------\n" +
                    " 9  .  .  .  .  .  .  .  .  . \n" +
                    " 8  .  X  X  .  .  .  .  .  . \n" +
                    " 7  .  .  O  .  .  .  .  .  . \n" +
                    " 6  .  .  .  .  .  .  .  .  . \n" +
                    " 5  .  .  .  O  .  .  .  .  . \n" +
                    " 4  .  .  .  .  .  .  .  .  . \n" +
                    " 3  .  .  .  .  .  .  .  .  . \n" +
                    " 2  .  .  .  .  .  .  .  .  . \n" +
                    " 1  .  .  .  .  .  .  .  .  . \n" +
                    "    A  B  C  D  E  F  G  H  I \n" +
                    "------------------------------", board.toString());

    }

    @Test
    void serializeABoard2() {
        var board = new GoBoard(9);

        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(WhitePlayer, new Point(3, 3));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(5, 4));



        assertEquals("------------------------------\n" +
                " 9  X  .  .  .  .  .  .  .  . \n" +
                " 8  .  .  X  .  .  .  .  .  . \n" +
                " 7  .  .  O  .  .  .  .  .  . \n" +
                " 6  .  .  .  .  .  .  .  .  . \n" +
                " 5  .  .  .  O  .  .  .  .  . \n" +
                " 4  .  .  .  .  .  .  .  .  . \n" +
                " 3  .  .  .  .  .  .  .  .  . \n" +
                " 2  .  .  .  .  .  .  .  .  . \n" +
                " 1  .  .  .  .  .  .  .  .  . \n" +
                "    A  B  C  D  E  F  G  H  I \n" +
                "------------------------------", board.toString());

    }


}
