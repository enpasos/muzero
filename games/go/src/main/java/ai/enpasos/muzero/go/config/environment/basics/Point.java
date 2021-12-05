/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package ai.enpasos.muzero.go.config.environment.basics;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Go board point class.
 * A point as modelled here is not aware of the boards boarders.
 * Therefore, it could be constructed off board. And so do the neighbours of a point.
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
