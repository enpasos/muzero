package ai.enpasos.muzero.pegsolitair.config.environment;


import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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

    static NeighborMap createNeighborMap() {
        int size = 7;
        var neighborMap = new NeighborMap();
        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                if (outOfBounds(row, col)) continue;
                var point = new Point(row, col);
                neighborMap.put(point, inRange(point.neighbors()));
            }
        }
        return neighborMap;
    }

    public static boolean outOfBounds(int row, int col) {
        return   (row < 3 && col < 3)
                || (row > 5 && col < 3)
                || (row < 3 && col > 5)
                || (row > 5 && col > 5)
                || row < 1 || col < 1 || row > 7 || col > 7;
    }


    public static boolean inRange(Point point) {
        return !outOfBounds(point.getRow(), point.getCol());
    }

    private static List<Point> inRange(List<Point> points) {
        return points.stream().filter(
                point -> inRange(point)
        ).collect(Collectors.toList());
    }

    List<Point> get(Point point) {
        return map.get(point);
    }

    void put(Point point, List<Point> list) {
        map.put(point, list);
    }





}
