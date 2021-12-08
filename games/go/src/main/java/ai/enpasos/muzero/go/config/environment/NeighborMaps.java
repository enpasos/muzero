package ai.enpasos.muzero.go.config.environment;

import java.util.HashMap;
import java.util.Map;

public class NeighborMaps {

    private static final Map<Integer, NeighborMap> directNeighborMaps = new HashMap<>();
    private static final Map<Integer, NeighborMap> diagonalNeighborMaps = new HashMap<>();

    private NeighborMaps() {}

    /**
     * @param size size of the board to create map for
     * @return a map from a point on the board to its (up to) 4 neighbors, for specified board size.
     */
    static NeighborMap getNbrTable(int size) {
        directNeighborMaps.computeIfAbsent(size, NeighborMap::createNeighborMap);
        return directNeighborMaps.get(size);
    }

    static NeighborMap getDiagonalTable(int size) {
        diagonalNeighborMaps.computeIfAbsent(size, NeighborMap::createDiagonalNeighborMap);
        return diagonalNeighborMaps.get(size);
    }


}
