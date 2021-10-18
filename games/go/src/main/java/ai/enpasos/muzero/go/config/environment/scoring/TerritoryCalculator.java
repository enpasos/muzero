package ai.enpasos.muzero.go.config.environment.scoring;


import ai.enpasos.muzero.go.config.environment.GoBoard;
import ai.enpasos.muzero.go.config.environment.basics.Player;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BlackPlayer;
import static ai.enpasos.muzero.go.config.environment.scoring.VertexType.*;

/**
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
public class TerritoryCalculator {
    private GoBoard goBoard;
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
                    var isBlack = playerOption.get() == BlackPlayer;
                    if (goBoard.getGoString(point).get().getLiberties().size() == 1) {
                        statusMap.put(point, isBlack ? CapturedBlackStone : CapturedWhiteStone);
                    } else {
                        statusMap.put(point, isBlack ? BlackStone : WhiteStone);
                    }
                }
            }
        }
        return statusMap;
    }

    private Map<Point, VertexType> categorizeTerritory(Map<Point, VertexType> pointToType)  {
        var statusMap = pointToType;

        for (int row = 1; row <= goBoard.getSize(); row++) {
            for  (int col = 1; col <= goBoard.getSize(); col++) {
                var point = new Point(row, col);
                var playerOption = goBoard.getPlayer(point);
                if (playerOption.isEmpty() || statusMap.get(point).isTerritory()) {
                    var group_neighbors = collectRegion(point, goBoard, statusMap);
                    var neighbors = group_neighbors.getRight();
                    var fillWith = (neighbors.size() == 1) ? // then all one color neighbors
                            (neighbors.first() == BlackPlayer ? BlackTerritory : WhiteTerritory)
                            : Dame;
                    var group = group_neighbors.getLeft();
                    group.stream()
                            .filter(p -> !statusMap.containsKey(p))
                            .forEach(pos -> statusMap.put(pos, fillWith));
                }
            }
        }

        return statusMap;
    }



    /**
     * @return (<list of points in the unoccupied area seeded by startingPint>,
     *          <players that own stones adjacent to this area>)
     *         If the region is bordered by only one player, then it is considered to be territory for that player.
     */
    private Pair<List<Point>, SortedSet<Player>> collectRegion(
            Point startingPoint,
            GoBoard board,
            Map<Point, VertexType> statusMap)  {
        var initialPlayer = board.getPlayer(startingPoint);
        // assert(initialPlayer.isEmpty() || statusMap.get(startingPoint).isTerritory())

        SortedSet<Player> visitedPlayers = new TreeSet<>();
        List<Point> visitedPoints = new ArrayList<>();
        visitedPoints.add(startingPoint);
        List<Point> nextPoints = new ArrayList<>();
        nextPoints.add(startingPoint);

        while (!nextPoints.isEmpty()) {
            var point = nextPoints.remove(0);
            var player = board.getPlayer(point);
            if (!player.isEmpty() && !statusMap.get(point).isTerritory())
                visitedPlayers.add(player.get());

            if (player.isEmpty() || statusMap.get(point).isTerritory()) {
                var nextVisits = point.neighbors().stream().filter(board::inBounds).collect(Collectors.toList());
                nextVisits.removeAll(visitedPoints);
                nextPoints = ListUtils.union(nextVisits, nextPoints);
                visitedPoints.add(point);
            }
        }



        return Pair.of(visitedPoints, visitedPlayers);
    }
}
