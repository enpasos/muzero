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

import ai.enpasos.mnist.blocks.ext.RescaleBlockExt;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel.*;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;

public class RepresentationOrDynamicsBlock extends MySequentialBlock {


    /**
     * "Both the representation and dynamics function use the same architecture
     * as AlphaZero, but with 16 instead of 20 residual blocks35. We use
     * 3 × 3 kernels and 256 hidden planes for each convolution."
     */


    public RepresentationOrDynamicsBlock(@NotNull MuZeroConfig config) {
        this(config.getNumResiduals(), config.getNumChannels(),config.getNumBottleneckChannels(),  config.getSqueezeChannelRatio(), config.getNumHiddenStateChannels(), config.getBroadcastEveryN());
    }


    public RepresentationOrDynamicsBlock(int numResiduals, int numChannels, int numBottleneckChannels, int squeezeChannelRatio, int numHiddenStateChannels, int broadcastEveryN) {


        this.add(Conv3x3.builder().channels(numChannels).build())

            .add(ResidualTower.builder()
                .numResiduals(numResiduals)
                .numChannels(numChannels)
                .numBottleneckChannels(numBottleneckChannels)
                .broadcastEveryN(broadcastEveryN)
                .squeezeChannelRatio(squeezeChannelRatio)
                .build())

            // compressing hidden state (not in muzero paper)
            .add(Conv1x1LayerNormRelu.builder().channels(numHiddenStateChannels).build())

            .add(new RescaleBlockExt());

    }

}
