package ai.enpasos.muzero.environments.go.environment;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Main Go board class. Represents the board on which Go moves can be played.
 * Internally, a grid keeps track of the strings at each vertex.
 *
 * @param size the size of the go board. Values of 5, 9, 13, 17, 19, or 25 are reasonable.
 * @param grid manages association of stones with parent strings.
 *             <p>
 *
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class GoBoard {
    @Builder.Default
    private Grid grid = new Grid();
    @Builder.Default
    private int blackCaptures = 0;
    @Builder.Default
    private int whiteCaptures = 0;
    private int size;
    private GoBoardBoundsChecker boundsChecker;
    private NeighborMap neighborMap;
    private NeighborMap diagonalMap;


    @Override
    public String toString() {
        return GoBoardSerializer.serialize(this);
    }

    public GoBoard(int size) {
        this.size = size;
        boundsChecker = GoBoardBoundsChecker.get(size);
        neighborMap = NeighborTables.getNbrTable(size);
        diagonalMap = NeighborTables.getDiagnonalTable(size);
        this.grid = new Grid();
        this.blackCaptures = 0;
        this.whiteCaptures = 0;
    }

    public GoBoard(int size, Grid grid, int blackCaptures, int whiteCaptures) {
        this(size);
        this.grid = grid;
        this.blackCaptures = blackCaptures;
        this.whiteCaptures = whiteCaptures;
    }

    /**
     * Main Go board class. Represents the board on which Go moves can be played. Immutable.
     * Internally, a grid keeps track of the strings at each vertex.
     *
     * @param size the size of the go board. Values of 5, 9, 13, 17, 19, or 25 are reasonable.
     * @param grid manages association of stones with parent strings.
     * @author Max Pumperla
     * @author Barry Becker
     */
//case class GoBoard(size: Int, grid: Grid = Grid(), blackCaptures: Int = 0, whiteCaptures: Int = 0) {

    public boolean inBounds(Point point) {
        return boundsChecker.inBounds(point);
    }

    public Optional<Player> getPlayer(Point point) {
        return grid.getPlayer(point);
    }

    public Optional<GoString> getGoString(Point point) {
        return grid.getString(point);
    }

    public Long zobristHash() {
        return grid.getHash();
    }


//    def placeStone(player: Player, point: Point): GoBoard = {
//        assert(boundsChecker.inBounds(point), point + " was not on the grid!")
//
//        if (grid.getString(point).isDefined) {
//            println(" Illegal move attempted at: " + point + ". Already occupied: " + grid.getString(point).get)
//            this
//        } else makeValidStonePlacement(player, point)
//    }

    public GoBoard placeStone(Player player, Point point) {
        if (!boundsChecker.inBounds(point)) {
            String message = point + " was not on the grid!";
            log.error(message);
            throw new RuntimeException(message);
        }
        if (grid.getString(point).isPresent()) {
            var message = "Illegal move attempted at: " + point + ". Already occupied: " + grid.getString(point).get();
            log.error(message);
            throw new RuntimeException(message);
        } else {
            return makeValidStonePlacement(player, point);
        }
    }

//
//    private def makeValidStonePlacement(player: Player, point: Point): GoBoard = {
//        var (newGrid, stringsToRemove) = determineStringsToRemove(player, point)
//
//        var newBlackCaptures = blackCaptures
//        var newWhiteCaptures = whiteCaptures
//        stringsToRemove.foreach(str => {
//                player match {
//        case BlackPlayer => newBlackCaptures += str.size
//        case WhitePlayer => newWhiteCaptures += str.size
//      }
//            newGrid = newGrid.removeString(str, neighborMap)
//    })
//
//            GoBoard(size, newGrid, newBlackCaptures, newWhiteCaptures)
//    }

    private GoBoard makeValidStonePlacement(Player player, Point point) {
        var pair = determineStringsToRemove(player, point);
        var stringsToRemove = pair.getRight();
        var newGrid = pair.getLeft();
        var newBlackCaptures = blackCaptures;
        var newWhiteCaptures = whiteCaptures;
        for (GoString str : stringsToRemove) {
            switch (player) {
                case BlackPlayer:
                    newBlackCaptures += str.size();
                    break;
                case WhitePlayer:
                    newWhiteCaptures += str.size();
            }
            newGrid = newGrid.removeString(str, this.neighborMap);
        }


        return new GoBoard(this.size, newGrid, newBlackCaptures, newWhiteCaptures);
    }


    public boolean isSelfCapture(Player player, Point point) {
        List<GoString> friendlyStrings = new ArrayList<>();

        for (Point neighbor : neighborMap.get(point)) {
            Optional<GoString> strOptional = grid.getString(neighbor);
            if (strOptional.isEmpty()) return false;
            GoString nbrStr = strOptional.get();

            if (nbrStr.getPlayer() == player) {
                friendlyStrings.add(nbrStr);
            } else if (nbrStr.numLiberties() == 1) {
                return false;
            }

        }


        for (GoString str : friendlyStrings) {
            if (str.numLiberties() != 1) return false;
        }
        return true;
    }

    private Pair<Grid, Set<GoString>> determineStringsToRemove(Player player, Point point) {
//            // 1. Examine adjacent points
        var adjacentSameColor = new HashSet<GoString>();
        var adjacentOppositeColor = new HashSet<GoString>();
        var liberties = new TreeSet<Point>();

        if (neighborMap.get(point) == null) {
            int i = 42;  // TODO: check if this should be possible
        }

        if (neighborMap.get(point) != null) {
            for (Point neighbor : neighborMap.get(point)) {
                grid.getString(neighbor).ifPresentOrElse(
                        str -> {
                            if (str.getPlayer() == player) {
                                adjacentSameColor.add(str);
                            } else {
                                adjacentOppositeColor.add(str);
                            }
                            return;
                        },
                        () -> liberties.add(neighbor)
                );
            }
        }

        // 2. Merge any strings of the same color adjacent to the placed stone
        adjacentSameColor.add(
                GoString.builder()
                        .player(player)
                        .stones(new TreeSet<Point>(List.of(point)))
                        .liberties(liberties)
                        .build());

        GoString newString = adjacentSameColor.stream().reduce(
                new GoString(player),
                GoString::mergedWith);

        var newGrid = grid.updateStringWhenAddingStone(point, newString);

        // 3. Reduce liberties of any adjacent strings of the opposite color.
        // 4. If any opposite color strings now have zero liberties, remove them.
        var stringsToRemove = new HashSet<GoString>();
        for (GoString otherColorString : adjacentOppositeColor) {
            var otherString = otherColorString.withoutLiberty(point);
            if (otherString.numLiberties() > 0) {
                newGrid = newGrid.replaceString(otherString);
            } else {
                stringsToRemove.add(otherString);
            }
        }

        return Pair.of(newGrid, stringsToRemove);
    }

    /**
     * A player should never fill her own eye, but determining a true eye is not that easy.
     *
     * @return true if the specified play fills that player's eye
     */
    boolean doesMoveFillEye(Player player, Point point) {
        int neighbors = findNumNeighbors(player, point, neighborMap);
        int diagNeighbors = findNumNeighbors(player, point, diagonalMap);
        int allNbrs = neighbors + diagNeighbors;

        if (boundsChecker.isCorner(point)) {
            return allNbrs == 3;
        } else if (boundsChecker.isEdge(point)) {
            return allNbrs == 5;
        } else {
            return neighbors == 4 && diagNeighbors >= 3;
        }

    }

    /**
     * @return the number of neighbors that are either the same color stone or an eye for that group
     */
    private int findNumNeighbors(Player player, Point point, NeighborMap nbrMap) {
        return (int) nbrMap.get(point).stream()
                .filter(nbr -> {
                    var str = grid.getString(nbr);
                    return (str.isPresent() && str.get().getPlayer() == player)
                            || (str.isEmpty() && isAncillaryEye(player, nbr));
                })
                .count();
    }


    private boolean isAncillaryEye(Player player, Point point) {

                var neighbors = neighborMap.findNumTrueNeighbors(player, point, grid);
                var diagNeighbors = diagonalMap.findNumTrueNeighbors(player, point, grid);
                var allNbrs = neighbors + diagNeighbors;

        if (boundsChecker.isCorner(point)) {
            return allNbrs == 2;
        } else if (boundsChecker.isEdge(point)) {
            return allNbrs == 4;
        } else {
            return neighbors == 4 && diagNeighbors >= 2;
        }

    }

    /**
     * @return true if player playing at point will capture stones
     */
    boolean willCapture(Player player, Point point) {
        for (Point nbr : neighborMap.get(point)) {
            var nbrStr = grid.getString(nbr);
            if (nbrStr.isPresent() && nbrStr.get().getPlayer() != player && nbrStr.get().numLiberties() == 1)
                return true;
        }
        return false;
    }


}
