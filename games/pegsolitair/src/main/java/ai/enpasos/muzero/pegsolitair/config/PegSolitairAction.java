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

package ai.enpasos.muzero.pegsolitair.config;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.pegsolitair.config.environment.Direction;
import ai.enpasos.muzero.pegsolitair.config.environment.Jump;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;

@Data
@ToString(onlyExplicitlyIncluded = true)
public class PegSolitairAction implements Comparable<PegSolitairAction>, Serializable, Action {

    @ToString.Include
    private int index;

    private transient MuZeroConfig config;

    public PegSolitairAction(MuZeroConfig config) {
        this.config = config;
    }

    public PegSolitairAction(MuZeroConfig config, int index) {
        this(config);
        this.index = index;
    }

//    public PegSolitairAction(@NotNull MuZeroConfig config, int row, int col) {
//        this(config, row * config.getBoardWidth() + col);
//    }
//
//    public static NDArray encodeEmptyNDArray(@NotNull MuZeroConfig config, @NotNull NDManager nd) {
//        return nd.zeros(new Shape(1, config.getBoardHeight(), config.getBoardWidth()));
//    }


    public Jump getJump() {
        return ActionAdapter.getJump(this);
    }

    @Override
    public int compareTo(@NotNull PegSolitairAction other) {
        return Integer.compare(this.index, other.index);
    }

    public NDArray encode(@NotNull NDManager nd) {
        NDArray array = nd.zeros(new Shape(Direction.values().length, config.getBoardHeight(), config.getBoardWidth()));
        array.setScalar(new NDIndex(getDirectionNumber(), getRow(), getCol()), 1f);
        return array;
    }


    public int getCol() {
        return getJump().getFromPoint().getCol()-1;
    }

    public int getRow() {
        return getJump().getFromPoint().getRow()-1;
    }

    public int getDirectionNumber() {
        return  getJump().getDirection().ordinal();
    }


}

