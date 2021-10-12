package ai.enpasos.muzero.environments.go.environment;

import ai.enpasos.muzero.environments.go.environment.basics.Point;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.environments.go.environment.basics.Player.BlackPlayer;
import static ai.enpasos.muzero.environments.go.environment.basics.Player.WhitePlayer;
import static org.junit.jupiter.api.Assertions.*;

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
