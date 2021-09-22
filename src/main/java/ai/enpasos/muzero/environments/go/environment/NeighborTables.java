package ai.enpasos.muzero.environments.go.environment;

import ai.enpasos.muzero.environments.go.environment.basics.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NeighborTables {

    private static Map<Integer, NeighborMap> neighborTables = new HashMap<>();
    private static Map<Integer, NeighborMap> diagonalTables = new HashMap<>();

    private NeighborTables() {
        // hide
    }

    /**
     * @param size size of the board to create map for
     * @return a map from a point on the board to its (up to) 4 neighbors, for specified board size.
     */
    static NeighborMap getNbrTable(int size) {
        if (!neighborTables.containsKey(size)) {
            neighborTables.put(size, createNeighborMap(size));
        }
        return neighborTables.get(size);
    }

    static NeighborMap getDiagnonalTable(int size) {
        if (!diagonalTables.containsKey(size)) {
            diagonalTables.put(size, createDiagonalNeighborMap(size));
        }
        return diagonalTables.get(size);
    }

    private static NeighborMap createNeighborMap(int size) {
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
    private static NeighborMap createDiagonalNeighborMap(int size) {
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

    private static List<Point> inRange(int size, List<Point> points) {
        return points.stream().filter(
                nbr -> 1 <= nbr.getRow() && nbr.getRow() <= size && 1 <= nbr.getCol() && nbr.getCol() <= size
        ).collect(Collectors.toList());
    }


}
