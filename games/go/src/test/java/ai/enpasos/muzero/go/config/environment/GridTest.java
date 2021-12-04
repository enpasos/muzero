package ai.enpasos.muzero.go.config.environment;

import ai.enpasos.muzero.go.config.environment.basics.Point;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.TreeSet;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BlackPlayer;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WhitePlayer;
import static org.testng.AssertJUnit.*;

public class GridTest {

    Grid grid;

    @BeforeTest
    void init() {
        grid = new Grid();
    }

    @Test
    void updateStringWhenAddingBlackStoneAt_2_2() {

        var string = simpleBlackString();
        var mygrid = grid.updateStringWhenAddingStone(new Point(2, 2), string);

        // should be black at 2, 3
        assertSame(mygrid.getPlayer(new Point(2, 3)).get(), BlackPlayer);
        assertEquals(mygrid.getString(new Point(2, 3)).get(), string);

        // should be empty elsewhere
        assertTrue(mygrid.getPlayer(new Point(1, 2)).isEmpty());
        assertTrue(mygrid.getPlayer(new Point(4, 4)).isEmpty());
        assertTrue(mygrid.getString(new Point(4, 4)).isEmpty());
    }

    /**
     * Suppose we have a grid like this
     * OOOO
     * OXXO
     * OO  The string of 2 black stones will be removed, and surrounding white string will have its liberties adjusted
     */
    @Test
    void removeStringDueToCaptureAndUpdateLibertiesInRemainingString() {

        var black2String = simpleBlackString();
        var white6String = GoString.builder()
                .player(WhitePlayer)
                .stones(new TreeSet<>(List.of(
                        new Point(1, 1),
                        new Point(1, 2),
                        new Point(1, 3),
                        new Point(2, 1),
                        new Point(2, 4)
                )))
                .liberties(new TreeSet<>(List.of(
                        new Point(2, 2),
                        new Point(2, 3),
                        new Point(2, 5),
                        new Point(1, 5),
                        new Point(3, 1),
                        new Point(3, 4)
                )))
                .build();

        var white2String = GoString.builder()
                .player(WhitePlayer)
                .stones(new TreeSet<>(List.of(
                        new Point(3, 2),
                        new Point(3, 3)
                )))
                .liberties(new TreeSet<>(List.of(
                        new Point(4, 2),
                        new Point(4, 3)
                )))
                .build();

        var mygrid = grid.updateStringWhenAddingStone(new Point(1, 2), white6String);

        // should have a black string should with 2 liberties initially
        mygrid = mygrid.updateStringWhenAddingStone(new Point(2, 2), black2String);
        // the black string is surrounded but not yet captured/removed
        assertSame(mygrid.getPlayer(new Point(2, 2)).get(), BlackPlayer);
        assertEquals(2, mygrid.getString(new Point(2, 3)).get().numLiberties());


        // should have a black string with liberties even when surrounded
        mygrid = mygrid.updateStringWhenAddingStone(new Point(3, 3), white2String);
        // the black string is surrounded but not yet captured/removed. Its liberties are not updated here.
        assertSame(mygrid.getPlayer(new Point(2, 2)).get(), BlackPlayer);
        assertSame(mygrid.getPlayer(new Point(2, 3)).get(), BlackPlayer);
        assertEquals(2, mygrid.getString(new Point(2, 3)).get().numLiberties()); // to be captured black string
        assertEquals(2, mygrid.getString(new Point(3, 3)).get().numLiberties()); // white string


        // should have a captured black string that is captured by surrounding white string
        // maps from point to list of neighbors
        var nbrMap = NeighborTables.getNbrTable(5);
        mygrid = mygrid.removeString(black2String, nbrMap);

        assertTrue(mygrid.getPlayer(new Point(2, 2)).isEmpty());
        assertTrue(mygrid.getPlayer(new Point(2, 3)).isEmpty());
        assertTrue(mygrid.getString(new Point(2, 3)).isEmpty());


        // should have surrounding white strings with more liberties after black's capture
        assertEquals(6, mygrid.getString(new Point(1, 2)).get().numLiberties());
        assertEquals(4, mygrid.getString(new Point(3, 3)).get().numLiberties());
        assertTrue(mygrid.getString(new Point(4, 4)).isEmpty());

    }


    private GoString simpleBlackString() {
        return GoString.builder()
                .player(BlackPlayer)
                .stones(new TreeSet<>(List.of(
                        new Point(2, 2),
                        new Point(2, 3)
                )))
                .liberties(new TreeSet<>(List.of(
                        new Point(3, 2),
                        new Point(3, 3)
                )))
                .build();
    }

    private GoString mediumWhiteString() {
        return GoString.builder()
                .player(WhitePlayer)
                .stones(new TreeSet<>(List.of(
                        new Point(1, 2),
                        new Point(2, 2),
                        new Point(3, 3),
                        new Point(2, 3)
                )))
                .liberties(new TreeSet<>(List.of(
                        new Point(1, 3),
                        new Point(2, 4),
                        new Point(4, 3)
                )))
                .build();
    }

    private Grid createGridWithStringAt22() {
        return grid.updateStringWhenAddingStone(new Point(2, 2), simpleBlackString());
    }
}
