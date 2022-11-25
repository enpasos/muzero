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

package ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.cmainfunctions;

import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel.ConcatInputsBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel.StartResidualBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
public class DynamicsBlock extends MySequentialBlock implements OnnxIO {


    public DynamicsBlock() {
        super();
    }


    @Builder()
    public static @NotNull DynamicsBlock newDynamicsBlock(MuZeroConfig config) {
        DynamicsBlock block = new DynamicsBlock();

        block
            .add(new StartResidualBlock(config.getNumChannels()))
            .add(new MainRepresentationOrDynamicsBlock(config, config.getNumResidualsGeneration(), config.getBroadcastEveryNGeneration()));

        return block;
    }

}
