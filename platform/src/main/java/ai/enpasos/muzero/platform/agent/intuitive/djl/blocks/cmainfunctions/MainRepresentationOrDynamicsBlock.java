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
import ai.enpasos.muzero.platform.config.NetworkType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
public class MainRepresentationOrDynamicsBlock extends MySequentialBlock {


    /**
     * "Both the representation and dynamics function use the same architecture
     * as AlphaZero, but with 16 instead of 20 residual blocks35. We use
     * 3 × 3 kernels and 256 hidden planes for each convolution."
     */


    public MainRepresentationOrDynamicsBlock(@NotNull MuZeroConfig config, int numResiduals, int broadcastEveryN) {
        this(config.getNetworkType(), config.getBoardHeight(), config.getBoardWidth(), numResiduals, config.getNumChannels(), config.getNumBottleneckChannels(),   broadcastEveryN );
    }

    @java.lang.SuppressWarnings("java:S107")
    public MainRepresentationOrDynamicsBlock(NetworkType networkType, int height, int width, int numResiduals, int numChannels, int numBottleneckChannels, int broadcastEveryN ) {
            this
//                .add(inputChannelsEqualsNumChannels ?
//                    new StartResidualBlock(numChannels) :
//                    Conv3x3.builder().channels(numChannels).build()
//                    )
                .add(ResidualTower.builder()
                    .numResiduals(numResiduals)
                    .numChannels(numChannels)
                    .numBottleneckChannels(numBottleneckChannels)
                    .broadcastEveryN(broadcastEveryN)

                    .height(height)
                    .width(width)
                    .build())

                .add(new RescaleBlockExt());

    }
}
