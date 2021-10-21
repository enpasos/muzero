package ai.enpasos.muzero.go.config.environment;

import ai.enpasos.muzero.go.config.environment.basics.Point;
import org.testng.annotations.Test;

import java.util.List;
import java.util.TreeSet;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BlackPlayer;
import static org.testng.AssertJUnit.assertEquals;


public class GoStringTest {

    @Test
    void createEmptyString() {
        var goString = new GoString(BlackPlayer);
        assertEquals(0, goString.numLiberties());
        assertEquals(0, goString.size());
        assertEquals("GoString(player=BlackPlayer, stones=[], liberties=[])", goString.toString());
    }

    @Test
    void createSimpleString() {
        var stones = new TreeSet<Point>(List.of(new Point(2, 2)));
        var liberties = new TreeSet<Point>(List.of(
                new Point(1, 2),
                new Point(3, 2),
                new Point(2, 1),
                new Point(2, 3)
        ));


        var goString = new GoString(BlackPlayer, stones, liberties);

        assertEquals(4, goString.numLiberties());

        assertEquals("GoString(player=BlackPlayer, stones=[Point(row=2, col=2)], liberties=[Point(row=1, col=2), Point(row=2, col=1), Point(row=2, col=3), Point(row=3, col=2)])", goString.toString());

    }


    // OOO
    // O O
    // OO    assume surrounded by black
    @Test
    void createStringWithOneEye() {
        var stones = new TreeSet<Point>(List.of(
                new Point(2, 2),
                new Point(2, 3),
                new Point(2, 4),
                new Point(3, 2),
                new Point(3, 4),
                new Point(4, 2),
                new Point(4, 3)
        ));

        var liberties = new TreeSet<Point>(List.of(new Point(3, 3)));

        var goString = new GoString(BlackPlayer, stones, liberties);

        assertEquals(1, goString.numLiberties());
    }

    @Test
    void addLibertyToString() {
        var stones = new TreeSet<Point>(List.of(
                new Point(2, 2)
        ));
        var liberties = new TreeSet<Point>(List.of(
                new Point(1, 2),
                new Point(3, 2)
        ));
        var goString = new GoString(BlackPlayer, stones, liberties);
        assertEquals(2, goString.numLiberties());
        assertEquals(3, goString.withLiberty(new Point(2, 1)).numLiberties());
    }

    @Test
    void removeLibertyFromString() {
        var stones = new TreeSet<Point>(List.of(
                new Point(2, 2)
        ));
        var liberties = new TreeSet<Point>(List.of(
                new Point(1, 2),
                new Point(3, 2),
                new Point(2, 1),
                new Point(2, 3)
        ));
        var goString = new GoString(BlackPlayer, stones, liberties);
        assertEquals(4, goString.numLiberties());
        var gs = goString.withoutLiberty(new Point(2, 1));
        assertEquals(3, gs.numLiberties());
        gs = gs.withoutLiberty(new Point(2, 3));
        assertEquals(2, gs.numLiberties());
    }


    @Test
    void mergeStrings() {
        var goString1 = GoString.builder()
                .player(BlackPlayer)
                .stones(new TreeSet<Point>(List.of(
                        new Point(2, 2)
                )))
                .liberties(new TreeSet<Point>(List.of(
                        new Point(1, 2),
                        new Point(2, 1),
                        new Point(3, 2),
                        new Point(2, 3)
                )))
                .build();

        var goString2 = GoString.builder()
                .player(BlackPlayer)
                .stones(new TreeSet<Point>(List.of(
                        new Point(2, 3)
                )))
                .liberties(new TreeSet<Point>(List.of(
                        new Point(2, 2),
                        new Point(2, 4),
                        new Point(1, 3),
                        new Point(3, 3)
                )))
                .build();

        assertEquals(4, goString1.numLiberties());
        assertEquals(4, goString2.numLiberties());

        var mergedString = goString1.mergedWith(goString2);
        assertEquals(6, mergedString.numLiberties());
        assertEquals("GoString(player=BlackPlayer, stones=[Point(row=2, col=2), Point(row=2, col=3)], liberties=[Point(row=1, col=2), Point(row=1, col=3), Point(row=2, col=1), Point(row=2, col=4), Point(row=3, col=2), Point(row=3, col=3)])", mergedString.toString());
    }

}
