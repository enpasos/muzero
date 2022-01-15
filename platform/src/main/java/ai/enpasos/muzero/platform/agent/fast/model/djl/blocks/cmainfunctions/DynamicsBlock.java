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

package ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.cmainfunctions;

import ai.djl.ndarray.types.Shape;
import ai.djl.nn.convolutional.Conv2d;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.ext.*;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel.ConcatInputsBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel.Conv1x1;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel.Conv1x1LayerNormRelu;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class DynamicsBlock  extends MySequentialBlock implements OnnxIO {


    public DynamicsBlock() {
    }


    @Builder()
    public static @NotNull DynamicsBlock newDynamicsBlock(MuZeroConfig config) {
        return (DynamicsBlock) new DynamicsBlock()
              .add(new ConcatInputsBlock())
             .add(new RepresentationOrDynamicsBlock(config));

    }

}
