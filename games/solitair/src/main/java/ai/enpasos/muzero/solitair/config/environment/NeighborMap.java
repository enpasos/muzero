package ai.enpasos.muzero.solitair.config.environment;


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
                var point = new Point(row, col);
                var allNeighbors = point.neighbors();
                var trueNeighbors = inRange(size, allNeighbors);
                neighborMap.put(new Point(row, col), trueNeighbors);
            }
        }
        return neighborMap;
    }



    // TODO correct
    private static List<Point> inRange(int size, List<Point> points) {
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





}
