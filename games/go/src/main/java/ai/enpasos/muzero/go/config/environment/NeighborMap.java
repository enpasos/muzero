package ai.enpasos.muzero.go.config.environment;


import ai.enpasos.muzero.go.config.environment.basics.Player;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps a point on a board grid to a set of other points on that same grid.
 * <p>
 * adapted from <a href="https://github.com/maxpumperla/ScalphaGoZero">...</a>
 */
@Data
public class NeighborMap {

    private Map<Point, List<Point>> map;

    public NeighborMap() {
        this.map = new HashMap<>();
    }

    static NeighborMap add(NeighborMap m1, NeighborMap m2) {
        NeighborMap m = new NeighborMap();
        m.map.putAll(m1.map);
        m.map.putAll(m2.map);
        return m;
    }

    static NeighborMap createNeighborMap(int size) {
        var neighborMap = new NeighborMap();
        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                var point = new Point(row, col);
                var allNeighbors = point.neighbors();
                var trueNeighbors = inRange(size, allNeighbors);
                neighborMap.put(new Point(row, col), trueNeighbors);
            }
        }
        return neighborMap;
    }

    /**
     * For each point in the grid, the map has the diagonals from that point
     */
    static NeighborMap createDiagonalNeighborMap(int size) {
        var diagonalMap = new NeighborMap();
        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                var point = new Point(row, col);
                var allDiagonals = point.diagonals();
                var trueDiagonals = inRange(size, allDiagonals);
                diagonalMap.put(new Point(row, col), trueDiagonals);
            }
        }
        return diagonalMap;
    }

    static List<Point> inRange(int size, List<Point> points) {
        return points.stream().filter(
            nbr -> 1 <= nbr.getRow() && nbr.getRow() <= size && 1 <= nbr.getCol() && nbr.getCol() <= size
        ).collect(Collectors.toList());
    }

    List<Point> get(Point point) {
        return map.get(point);
    }

    void put(Point point, List<Point> list) {
        map.put(point, list);
    }

    int findNumTrueNeighbors(Player player, Point point, Grid grid) {
        return (int) this.map.get(point).stream().filter(
                neighbor -> {
                    var str = grid.getString(neighbor);
                    return str.isPresent() && str.get().getPlayer() == player;
                })
            .count();
    }


}
