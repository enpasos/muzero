package ai.enpasos.muzero.go.config.environment;

import ai.enpasos.muzero.go.config.environment.basics.Point;

import java.util.HashMap;
import java.util.Map;

public class GoBoardBoundsChecker {
    private static final Map<Integer, GoBoardBoundsChecker> map = new HashMap<>();
    private final int size;


    GoBoardBoundsChecker(int size) {
        this.size = size;
    }

    static GoBoardBoundsChecker get(int size) {
        map.computeIfAbsent(size, GoBoardBoundsChecker::new);
        return map.get(size);

    }

    boolean inBounds(Point point) {
        return 1 <= point.getRow() && point.getRow() <= size && 1 <= point.getCol() && point.getCol() <= size;
    }

    boolean isCorner(Point point) {
        return (point.getRow() == 1 && point.getCol() == 1) ||
            (point.getRow() == size && point.getCol() == 1) ||
            (point.getRow() == 1 && point.getCol() == size) ||
            (point.getRow() == size && point.getCol() == size);
    }

    boolean isEdge(Point point) {
        return point.getRow() == 1 || point.getCol() == 1 || point.getRow() == size || point.getCol() == size;
    }

}





