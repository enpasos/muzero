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

package ai.enpasos.muzero.agent.fast.model.djl.blocks.cmainfunctions;

import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.dlowerlevel.Conv3x3BatchNormRelu;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.dlowerlevel.MySequentialBlock;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.dlowerlevel.RescaleBlock;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.dlowerlevel.ResidualTower;
import org.jetbrains.annotations.NotNull;

public class RepresentationOrDynamicsBlock extends MySequentialBlock {


    /**
     * "Both the representation and dynamics function use the same architecture
     * as AlphaZero, but with 16 instead of 20 residual blocks35. We use
     * 3 × 3 kernels and 256 hidden planes for each convolution."
     */

    public RepresentationOrDynamicsBlock(@NotNull MuZeroConfig config) {

        this.add(Conv3x3BatchNormRelu.builder().channels(config.getNumChannels()).build())

                .add(ResidualTower.builder()
                        .numResiduals(config.getNumResiduals())
                        .numChannels(config.getNumChannels())
                        .build())

                .add(new RescaleBlock());

    }

}
