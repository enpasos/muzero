package ai.enpasos.muzero.environments.go.environment;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


import static ai.enpasos.muzero.environments.go.environment.ZobristHashing.ZOBRIST;

/**
 * Keeps track of parent string for each location on the board where there is a stone.
 *
 * @param grid map from grid point location to parent string of stones (if any)
 * @param hash the Zobrist hash. Gets updated as moves are played. Used to detect ko.
 *
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
@Data
@AllArgsConstructor
public class Grid {

    private Map<Point, GoString> grid;
    private Long hash;

    Grid() {
        hash = 0L;
        grid = new HashMap<>();
    }

    Optional<GoString> getString(Point point) {
        return Optional.ofNullable(grid.get(point));
    }

    Optional<Player> getPlayer(Point point) {
        return getString(point).map(GoString::getPlayer);
    }



    /**
     * @param point the position of the stone that was added
     * @param newString the new parent string for that added stone
     */
    Grid updateStringWhenAddingStone(Point point, GoString newString)  {
        // TODO check if reference to old HashMap object is sufficient
        Map<Point, GoString> newGrid = new HashMap<>();
        newGrid.putAll(grid);
       // assert(newString.stones.contains(point))

        newString.getStones().stream().forEach(
                newStringPoint -> newGrid.put(newStringPoint, newString)
        );


        var newHash = hash;
        newHash ^= ZOBRIST.get(Pair.of(point, Optional.empty())); // Remove empty-point hash code
        newHash ^= ZOBRIST.get(Pair.of(point, Optional.of(newString.getPlayer()))); // Add filled point hash code.

        return new Grid(newGrid, newHash);
    }

    Grid replaceString(GoString newString) {
        return new Grid(replaceString(newString, grid), hash);
    }


    private Map<Point, GoString>  replaceString(GoString newString, Map<Point, GoString> g) {
        var newGrid = g;  // it is not new but modified !!!!!!
        newString.getStones().forEach(
                point -> newGrid.put(point, newString)
        );
        return newGrid;
    }

    /**
     * When a string is removed due to capture, also update the liberties of the adjacent strings of opposite color.
     * @param removedString the string to remove
     * @return newGrid and newHash value
     */
    Grid removeString(GoString removedString, NeighborMap nbrMap)  {
        var newGrid = grid;
        var newHash = hash;

        // first remove the stones from the board
        for(Point point : removedString.getStones())
                {
                        newGrid.remove(point);  // the point is now empty
            newHash ^= ZOBRIST.get(Pair.of(point, Optional.of(removedString.getPlayer()))); // Remove filled point hash code.
            newHash ^= ZOBRIST.get(Pair.of(point, Optional.empty())); // Add empty point hash code.
        }

        // for each opponent neighbor string adjacent to the one removed, add a liberty for each removed point
        for(Point point : removedString.getStones()) {
            for(Point neighbor : nbrMap.get(point))
            {
                var oppNbrString = newGrid.get(neighbor);
                if (oppNbrString != null) {
                    newGrid = replaceString(oppNbrString.withLiberty(point), newGrid);
                }
            }
        }
        return new Grid(newGrid, newHash);
    }

}
