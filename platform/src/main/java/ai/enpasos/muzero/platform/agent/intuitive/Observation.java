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

package ai.enpasos.muzero.platform.agent.intuitive;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;


@Data
@EqualsAndHashCode
public class Observation {
    private float[] value;
    private long[] shape;

    public Observation(float[] value, long[] shape) {
        this.value = value;
        this.shape = shape;
    }


    @SuppressWarnings({"java:S2095"})
    public NDArray getNDArray(@NotNull NDManager ndManager) {
        return ndManager.create(value).reshape(shape);
    }


}
