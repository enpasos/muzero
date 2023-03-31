package ai.enpasos.muzero.go.config.environment;

import ai.enpasos.muzero.go.config.environment.basics.Point;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BLACK_PLAYER;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WHITE_PLAYER;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GoBoardSerializerTest {

    @Test
    void serializeABoard1() {
        var board = new GoBoard(9);

        board = board.placeStone(BLACK_PLAYER, new Point(8, 2));
        board = board.placeStone(BLACK_PLAYER, new Point(8, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(7, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 4));

        assertEquals("""
            ------------------------------
             9  .  .  .  .  .  .  .  .  .\s
             8  .  X  X  .  .  .  .  .  .\s
             7  .  .  O  .  .  .  .  .  .\s
             6  .  .  .  .  .  .  .  .  .\s
             5  .  .  .  O  .  .  .  .  .\s
             4  .  .  .  .  .  .  .  .  .\s
             3  .  .  .  .  .  .  .  .  .\s
             2  .  .  .  .  .  .  .  .  .\s
             1  .  .  .  .  .  .  .  .  .\s
                A  B  C  D  E  F  G  H  I\s
            ------------------------------""", board.toString());

    }

    @Test
    void serializeABoard2() {
        var board = new GoBoard(9);

        board = board.placeStone(BLACK_PLAYER, new Point(9, 1));
        board = board.placeStone(BLACK_PLAYER, new Point(8, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(7, 3));
        board = board.placeStone(WHITE_PLAYER, new Point(5, 4));


        assertEquals("""
            ------------------------------
             9  X  .  .  .  .  .  .  .  .\s
             8  .  .  X  .  .  .  .  .  .\s
             7  .  .  O  .  .  .  .  .  .\s
             6  .  .  .  .  .  .  .  .  .\s
             5  .  .  .  O  .  .  .  .  .\s
             4  .  .  .  .  .  .  .  .  .\s
             3  .  .  .  .  .  .  .  .  .\s
             2  .  .  .  .  .  .  .  .  .\s
             1  .  .  .  .  .  .  .  .  .\s
                A  B  C  D  E  F  G  H  I\s
            ------------------------------""", board.toString());

    }


}
