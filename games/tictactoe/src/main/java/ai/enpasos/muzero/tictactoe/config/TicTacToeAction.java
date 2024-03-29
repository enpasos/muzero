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

package ai.enpasos.muzero.tictactoe.config;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuppressWarnings("squid:S2065")
public class TicTacToeAction extends Action {

    private transient MuZeroConfig config;

    public TicTacToeAction(MuZeroConfig config) {
        this.config = config;
    }


    public static int getCol(@NotNull MuZeroConfig config, int index) {
        return index % config.getBoardWidth();
    }

    public static int getRow(@NotNull MuZeroConfig config, int index) {
        return (index - getCol(config, index)) / config.getBoardWidth();
    }

    @Override
    public int compareTo(@NotNull Action other) {
        return Integer.compare(this.getIndex(), other.getIndex());
    }

    public NDArray encode(@NotNull NDManager nd) {
        NDArray array = nd.zeros(new Shape(1, config.getBoardHeight(), config.getBoardWidth()));
        array.setScalar(new NDIndex(0, getRow(), getCol()), 1f);
        return array;
    }

    public int getCol() {
        return getCol(config, getIndex());
    }

    public int getRow() {
        return getRow(config, getIndex());
    }


}

