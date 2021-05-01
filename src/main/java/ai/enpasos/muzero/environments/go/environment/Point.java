package ai.enpasos.muzero.environments.go.environment;


import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Go board point class.
 * <p>
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
@Data
public class Point implements Comparable<Point> {

    private int row;
    private int col;

    public Point(int row, int col) {
        this.row = row;
        this.col = col;
    }

    // def this(tuple: (Int, Int)) = this(tuple._1, tuple._2)

    /**
     * @return strongly connected neighbors
     */
   public List<Point> neighbors() {
        return List.of(
                new Point(row - 1, col),
                new Point(row + 1, col),
                new Point(row, col - 1),
                new Point(row, col + 1)
        );
    }

    /**
     * @return adjacent diagonals from this point
     */
    public List<Point> diagonals() {
        return List.of(
                new Point(row - 1, col - 1),
                new Point(row + 1, col + 1),
                new Point(row - 1, col + 1),
                new Point(row + 1, col - 1)
        );
    }

    @Override
    public int compareTo(@NotNull Point point) {
        int c = Integer.compare(this.row, point.row);
        if (c == 0) {
            c = Integer.compare(this.col, point.col);
        }
        return c;
    }
}
