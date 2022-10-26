package ai.enpasos.muzero.go.config.environment.scoring;


import ai.enpasos.muzero.go.config.environment.GoBoard;
import ai.enpasos.muzero.go.config.environment.basics.Player;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import ai.enpasos.muzero.platform.common.MuZeroException;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BLACK_PLAYER;
import static ai.enpasos.muzero.go.config.environment.scoring.VertexType.*;

/**
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
@SuppressWarnings({"squid:S3776", "squid:S3358"})
public class TerritoryCalculator {
    private final GoBoard goBoard;

    public TerritoryCalculator(GoBoard goBoard) {
        this.goBoard = goBoard;
    }


    /**
     * Evaluate / estimate the territory currently on the Go board
     * Stones that belong to a string that is in atari, are considered dead and captured.
     * The reason this can happen is that the computer will always play valid moves until there are no more valid moves.
     * Since suicide is not legal, it will play until a group is in atari, then stop.
     * A human may stop playing before that happen, so at the time when both players pass, there may be
     * some stones in atari. Those stones need to be removed and counted as captures.
     *
     * @return Territory object
     */
    Map<Point, VertexType> evaluateTerritory() {

        // Make 2 passes. In first pass, categorize all stones
        var statusMap = categorizeStones();
        // In second pass, mark all the empty regions, and dead stones, as belonging to a players territory.
        return categorizeTerritory(statusMap);
    }

    /**
     * First add all the stones on the board to the map with a proper categorization.
     * i.e. either live stone or captured stone. A stone is considered captured if part of a string in atari.
     */
    private Map<Point, VertexType> categorizeStones() {
        var statusMap = new TreeMap<Point, VertexType>();

        for (int row = 1; row <= goBoard.getSize(); row++) {
            for (int col = 1; col <= goBoard.getSize(); col++) {
                var point = new Point(row, col);
                var playerOption = goBoard.getPlayer(point);
                if (playerOption.isPresent()) {
                    var isBlack = playerOption.orElseThrow(MuZeroException::new) == BLACK_PLAYER;
                    if (goBoard.getGoString(point).orElseThrow(MuZeroException::new).getLiberties().size() == 1) {
                        statusMap.put(point, isBlack ? CAPTURED_BLACK_STONE : CAPTURED_WHITE_STONE);
                    } else {
                        statusMap.put(point, isBlack ? BLACK_STONE : WHITE_STONE);
                    }
                }
            }
        }
        return statusMap;
    }

    private Map<Point, VertexType> categorizeTerritory(Map<Point, VertexType> pointToType) {

        for (int row = 1; row <= goBoard.getSize(); row++) {
            for (int col = 1; col <= goBoard.getSize(); col++) {
                var point = new Point(row, col);
                var playerOption = goBoard.getPlayer(point);
                if (playerOption.isEmpty() || pointToType.get(point).isTerritory()) {
                    var groupNeighbors = collectRegion(point, goBoard, pointToType);
                    var neighbors = groupNeighbors.getRight();
                    var fillWith = (neighbors.size() == 1) ? // then all one color neighbors
                        (neighbors.first() == BLACK_PLAYER ? BLACK_TERRITORY : WHITE_TERRITORY)
                        : DAME;
                    var group = groupNeighbors.getLeft();
                    group.stream()
                        .filter(p -> !pointToType.containsKey(p))
                        .forEach(pos -> pointToType.put(pos, fillWith));
                }
            }
        }

        return pointToType;
    }


    /**
     * @return (< list of points in the unoccupied area seeded by startingPint >,
     * <players that own stones adjacent to this area>)
     * If the region is bordered by only one player, then it is considered to be territory for that player.
     */
    private Pair<List<Point>, SortedSet<Player>> collectRegion(
        Point startingPoint,
        GoBoard board,
        Map<Point, VertexType> statusMap) {

        SortedSet<Player> visitedPlayers = new TreeSet<>();
        List<Point> visitedPoints = new ArrayList<>();
        visitedPoints.add(startingPoint);
        List<Point> nextPoints = new ArrayList<>();
        nextPoints.add(startingPoint);

        while (!nextPoints.isEmpty()) {
            var point = nextPoints.remove(0);
            var player = board.getPlayer(point);
            if (player.isPresent() && !statusMap.get(point).isTerritory())
                visitedPlayers.add(player.get());

            if (player.isEmpty() || statusMap.get(point).isTerritory()) {
                var nextVisits = point.neighbors().stream().filter(board::inBounds).collect( Collectors.toList());
                nextVisits.removeAll(visitedPoints);
                nextPoints = ListUtils.union(nextVisits, nextPoints);
                visitedPoints.add(point);
            }
        }


        return Pair.of(visitedPoints, visitedPlayers);
    }
}
