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

package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel;

import ai.djl.nn.Block;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.CausalityFreezing;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
    public class CausalResidualTower extends MySequentialBlock implements CausalityFreezing {

    private CausalResidualTower() {
    }

    @Builder()
    public static @NotNull CausalResidualTower newCausalResidualTower(int height, int width, int numResiduals,  int[] numChannels, int[] numCompressedChannels,  int broadcastEveryN ) {
        CausalResidualTower instance = new CausalResidualTower();
        for (int i = 0; i < numResiduals; i++) {
            boolean rescale = false;
            if (i == numResiduals - 1  ) rescale = true;

            if (i % broadcastEveryN == broadcastEveryN - 1) {
                instance.add(new CausalBroadcastResidualLayersBlock(height, width, numChannels,  numCompressedChannels, rescale));
            } else {
                instance.add(new CausalBottleneckResidualLayersBlock( numChannels,  numCompressedChannels, rescale));
            }
        }
        return instance;
    }

    @Override
    public void freeze(boolean[] freeze) {
        this.getChildren().forEach(b -> {
            if (b.getValue() instanceof CausalityFreezing) {
                ((CausalityFreezing) b.getValue()).freeze(freeze);
            }
        });
    }

    public Block getBlockForInitialRulesOnly(MuZeroConfig config) {
        int numResiduals = config.getNumResiduals();
        int broadcastEveryN = config.getBroadcastEveryN();
        int height = config.getBoardHeight();
        int width = config.getBoardWidth();
        int[] numChannels = new int[] {config.getNumChannelsRulesInitial(), config.getNumChannelsRulesRecurrent(),  config.getNumChannelsPolicy(), config.getNumChannelsValue()};
        int[] numCompressedChannels = new int[] {config.getNumCompressedChannelsRulesInitial(),config.getNumCompressedChannelsRulesRecurrent(),  config.getNumCompressedChannelsPolicy(), config.getNumCompressedChannelsValue()};

        CausalResidualTower instance = new CausalResidualTower();
        int c = 0;
        for (int i = 0; i < numResiduals; i++) {
            Block child = getChildren().get(c).getValue();
            if(child instanceof CausalBroadcastResidualLayersBlock specialChild) {
                instance.add(specialChild.getBlockForInitialRulesOnly());
            } else if(child instanceof CausalBottleneckResidualLayersBlock specialChild) {
                instance.add(specialChild.getBlockForInitialRulesOnly());
            }

        }
        return instance;
    }
}
