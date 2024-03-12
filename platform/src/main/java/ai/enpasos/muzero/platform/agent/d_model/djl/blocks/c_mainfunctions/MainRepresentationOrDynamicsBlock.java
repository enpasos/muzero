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

import ai.djl.nn.Block;
import ai.djl.util.Pair;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.CausalResidualTower;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
public class MainRepresentationOrDynamicsBlock extends MySequentialBlock implements DCLAware {

    private  int noOfActiveLayers;

    public int getNoOfActiveLayers() {
        return noOfActiveLayers;
    }

    public void setNoOfActiveLayers(int noOfActiveLayers) {
        this.noOfActiveLayers = noOfActiveLayers;
        for (Pair<String, Block> child : this.children) {
            if(child.getValue() instanceof DCLAware dclAware) {
                dclAware.setNoOfActiveLayers(noOfActiveLayers);
            }
        }
    }


    /**
     * "Both the representation and dynamics function use the same architecture
     * as AlphaZero, but with 16 instead of 20 residual blocks35. We use
     * 3 × 3 kernels and 256 hidden planes for each convolution."
     */


   private MainRepresentationOrDynamicsBlock( CausalResidualTower causalResidualTower ) {
       this.causalResidualTower = causalResidualTower;
       this.add(causalResidualTower);
        }

    public MainRepresentationOrDynamicsBlock(@NotNull MuZeroConfig config  ) {
        this(config.getBoardHeight(), config.getBoardWidth(), config.getNumResiduals(), new int[] {config.getNumChannelsRulesInitial(), config.getNumChannelsRulesRecurrent(),  config.getNumChannelsPolicy(), config.getNumChannelsValue()}, new int[] {config.getNumCompressedChannelsRulesInitial(),config.getNumCompressedChannelsRulesRecurrent(),  config.getNumCompressedChannelsPolicy(), config.getNumCompressedChannelsValue()}, config.getBroadcastEveryN());
    }

  final private CausalResidualTower causalResidualTower;

    @java.lang.SuppressWarnings("java:S107")
    public MainRepresentationOrDynamicsBlock(int height, int width, int numResiduals, int[] numChannels,  int[] numCompressedChannels,   int broadcastEveryN) {

        causalResidualTower = CausalResidualTower.builder()
                .numResiduals(numResiduals)
                .numChannels(numChannels)
                .numCompressedChannels (numCompressedChannels)
                .broadcastEveryN(broadcastEveryN)
                .height(height)
                .width(width)

                .build();

        this.add(causalResidualTower);

    }


    @Override
    public void freeze(boolean[] freeze) {
        this.getChildren().forEach(b -> {
            if (b.getValue() instanceof DCLAware) {
                ((DCLAware) b.getValue()).freeze(freeze);
            }
        });
    }

    public Block getBlockForInitialRulesOnly( MuZeroConfig config) {
        return new MainRepresentationOrDynamicsBlock(causalResidualTower.getBlockForInitialRulesOnly(config));

    }
}
