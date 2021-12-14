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

package ai.enpasos.muzero.platform.agent.slow.play;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import lombok.Data;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;


@Data
@ToString(onlyExplicitlyIncluded = true)
public abstract class Action implements Comparable<Action> {

    @ToString.Include
    private int index;

    public abstract NDArray encode(NDManager nd);

    @Override
    public int compareTo(@NotNull Action other) {
        return Integer.compare(this.getIndex(), other.getIndex());
    }

}

