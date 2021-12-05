package ai.enpasos.muzero.go.config.environment;

import java.util.HashMap;
import java.util.Map;

import static ai.enpasos.muzero.go.config.environment.NeighborMap.createDiagonalNeighborMap;
import static ai.enpasos.muzero.go.config.environment.NeighborMap.createNeighborMap;

public class NeighborTables {

    private static final Map<Integer, NeighborMap> neighborTables = new HashMap<>();
    private static final Map<Integer, NeighborMap> diagonalTables = new HashMap<>();

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

    static NeighborMap getDiagonalTable(int size) {
        if (!diagonalTables.containsKey(size)) {
            diagonalTables.put(size, createDiagonalNeighborMap(size));
        }
        return diagonalTables.get(size);
    }


}
