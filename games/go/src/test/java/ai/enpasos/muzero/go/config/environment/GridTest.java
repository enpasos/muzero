package ai.enpasos.muzero.go.config.environment;

import ai.enpasos.muzero.go.config.environment.GoString;
import ai.enpasos.muzero.go.config.environment.Grid;
import ai.enpasos.muzero.go.config.environment.NeighborTables;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.TreeSet;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BlackPlayer;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WhitePlayer;
import static org.testng.AssertJUnit.assertTrue;

class GridTest {

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
        assertTrue(mygrid.getPlayer(new Point(2, 3)).get() == BlackPlayer);
        assertTrue(mygrid.getString(new Point(2, 3)).get().equals(string));

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
                .stones(new TreeSet<Point>(List.of(
                        new Point(1, 1),
                        new Point(1, 2),
                        new Point(1, 3),
                        new Point(2, 1),
                        new Point(2, 4)
                )))
                .liberties(new TreeSet<Point>(List.of(
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
                .stones(new TreeSet<Point>(List.of(
                        new Point(3, 2),
                        new Point(3, 3)
                )))
                .liberties(new TreeSet<Point>(List.of(
                        new Point(4, 2),
                        new Point(4, 3)
                )))
                .build();

        var mygrid = grid.updateStringWhenAddingStone(new Point(1, 2), white6String);

        // should have a black string should with 2 liberties initially
        mygrid = mygrid.updateStringWhenAddingStone(new Point(2, 2), black2String);
        // the black string is surrounded but not yet captured/removed
        assertTrue(mygrid.getPlayer(new Point(2, 2)).get() == BlackPlayer);
        assertTrue(mygrid.getString(new Point(2, 3)).get().numLiberties() == 2);


        // should have a black string with liberties even when surrounded
        mygrid = mygrid.updateStringWhenAddingStone(new Point(3, 3), white2String);
        // the black string is surrounded but not yet captured/removed. Its liberties are not updated here.
        assertTrue(mygrid.getPlayer(new Point(2, 2)).get() == BlackPlayer);
        assertTrue(mygrid.getPlayer(new Point(2, 3)).get() == BlackPlayer);
        assertTrue(mygrid.getString(new Point(2, 3)).get().numLiberties() == 2); // to be captured black string
        assertTrue(mygrid.getString(new Point(3, 3)).get().numLiberties() == 2); // white string


        // should have a captured black string that is captured by surrounding white string
        // maps from point to list of neighbors
        var nbrMap = NeighborTables.getNbrTable(5);
        mygrid = mygrid.removeString(black2String, nbrMap);

        assertTrue(mygrid.getPlayer(new Point(2, 2)).isEmpty());
        assertTrue(mygrid.getPlayer(new Point(2, 3)).isEmpty());
        assertTrue(mygrid.getString(new Point(2, 3)).isEmpty());


        // should have surrounding white strings with more liberties after black's capture
        assertTrue(mygrid.getString(new Point(1, 2)).get().numLiberties() == 6);
        assertTrue(mygrid.getString(new Point(3, 3)).get().numLiberties() == 4);
        assertTrue(mygrid.getString(new Point(4, 4)).isEmpty());

    }


    private GoString simpleBlackString() {
        return GoString.builder()
                .player(BlackPlayer)
                .stones(new TreeSet<Point>(List.of(
                        new Point(2, 2),
                        new Point(2, 3)
                )))
                .liberties(new TreeSet<Point>(List.of(
                        new Point(3, 2),
                        new Point(3, 3)
                )))
                .build();
    }

    private GoString mediumWhiteString() {
        return GoString.builder()
                .player(WhitePlayer)
                .stones(new TreeSet<Point>(List.of(
                        new Point(1, 2),
                        new Point(2, 2),
                        new Point(3, 3),
                        new Point(2, 3)
                )))
                .liberties(new TreeSet<Point>(List.of(
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
