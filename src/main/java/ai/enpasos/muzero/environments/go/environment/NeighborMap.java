package ai.enpasos.muzero.environments.go.environment;


import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a point on a board grid to a set of other points on that same grid.
 *
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
@Data
public class NeighborMap {

        private Map<Point, List<Point>> map;

        public NeighborMap() {
                this.map = new HashMap<>();
        }

        List<Point> get(Point point) {
                return map.get(point);
        }

        void put(Point point, List<Point> list) {
                map.put(point, list);
        }


        static NeighborMap add(NeighborMap m1, NeighborMap m2)  {
                NeighborMap m = new NeighborMap();
                m.map.putAll(m1.map);
                m.map.putAll(m2.map);
                return m;
        }


       int findNumTrueNeighbors(Player player, Point point, Grid grid) {
             return (int)this.map.get(point).stream().filter(
                        neighbor -> {
                             var str = grid.getString(neighbor);
                             return str.isPresent() && str.get().getPlayer() == player;
                        })
                     .count();
       }

}
