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

package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions;

import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.CausalResidualTower;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.CausalityFreezing;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
public class MainRepresentationOrDynamicsBlock extends MySequentialBlock implements CausalityFreezing {


    /**
     * "Both the representation and dynamics function use the same architecture
     * as AlphaZero, but with 16 instead of 20 residual blocks35. We use
     * 3 × 3 kernels and 256 hidden planes for each convolution."
     */


    public MainRepresentationOrDynamicsBlock(@NotNull MuZeroConfig config, int numResiduals, int broadcastEveryN) {
        this(config.getBoardHeight(), config.getBoardWidth(), numResiduals, config.getNumChannelsRules(),  config.getNumChannelsPolicy(), config.getNumChannelsValue(), broadcastEveryN);
    }

    @java.lang.SuppressWarnings("java:S107")
    public MainRepresentationOrDynamicsBlock(int height, int width, int numResiduals, int numChannelsRules, int numChannelsPolicy,  int numChannelsValue, int broadcastEveryN) {
        this.add(CausalResidualTower.builder()
                .numResiduals(numResiduals)
                .numChannelsRules(numChannelsRules)
                .numChannelsPolicy(numChannelsPolicy)
                .numChannelsValue(numChannelsValue)
              //  .numBottleneckChannels(numBottleneckChannels)
                .broadcastEveryN(broadcastEveryN)
                .height(height)
                .width(width)
                .rescaleOnEnd(true)
                .build());

    }


    @Override
    public void freeze(boolean[] freeze) {
        this.getChildren().forEach(b -> {
            if (b.getValue() instanceof CausalityFreezing) {
                ((CausalityFreezing) b.getValue()).freeze(freeze);
            }
        });
    }
}
