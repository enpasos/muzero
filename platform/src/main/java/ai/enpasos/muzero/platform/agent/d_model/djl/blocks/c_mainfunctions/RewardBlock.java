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

import ai.enpasos.mnist.blocks.ext.*;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.*;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@SuppressWarnings("java:S110")
public class RewardBlock extends MySequentialBlock {

    public RewardBlock(@NotNull MuZeroConfig config) {
        boolean isPlayerModeTWOPLAYERS =
                config.getPlayerMode() == PlayerMode.TWO_PLAYERS ;



        SequentialBlockExt reward = new SequentialBlockExt();


        reward.add(new ConcatInputsBlock());
        reward.add(Conv3x3.builder().channels(config.getNumChannels4()).build());
        reward.add(new MainRepresentationOrDynamicsBlock(config.getBoardHeight(), config.getBoardWidth(), config.getNumResiduals4(), config.getNumChannels4(), config.getNumBottleneckChannels4(), config.getBroadcastEveryN4()));



        reward.add(Conv1x1LayerNormRelu.builder().channels(1).build())
                .add(BlocksExt.batchFlattenBlock());
        reward.add(LinearExt.builder()
                        .setUnits(config.getNumChannelsReward()) // config.getNumChannels())  // originally 256
                        .build())
                .add(ActivationExt.reluBlock());
        reward.add(LinearExt.builder()
                .setUnits(1).build());
        if (isPlayerModeTWOPLAYERS) {
            reward.add(ActivationExt.tanhBlock());
        }


        add(reward
        );
    }


}
