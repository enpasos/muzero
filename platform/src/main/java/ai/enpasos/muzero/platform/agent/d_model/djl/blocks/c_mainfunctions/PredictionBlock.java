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

import ai.enpasos.mnist.blocks.ext.ActivationExt;
import ai.enpasos.mnist.blocks.ext.BlocksExt;
import ai.enpasos.mnist.blocks.ext.LinearExt;
import ai.enpasos.mnist.blocks.ext.ParallelBlockWithCollectChannelJoinExt;
import ai.enpasos.mnist.blocks.ext.SequentialBlockExt;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.Conv1x1LayerNormRelu;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@SuppressWarnings("java:S110")
public class PredictionBlock extends MySequentialBlock {

    public PredictionBlock(@NotNull MuZeroConfig config) {
        this(  config.getNumChannels(), config.getPlayerMode() == PlayerMode.TWO_PLAYERS, config.getActionSpaceSize());
    }

    public PredictionBlock(int numChannels, boolean isPlayerModeTWOPLAYERS, int actionSpaceSize) {


        SequentialBlockExt valueHead = new SequentialBlockExt();
        valueHead.add(Conv1x1LayerNormRelu.builder().channels(1).build())
                .add(BlocksExt.batchFlattenBlock());
        valueHead.add(LinearExt.builder()
                        .setUnits(numChannels) // config.getNumChannels())  // originally 256
                        .build())
                .add(ActivationExt.reluBlock());
        valueHead.add(LinearExt.builder()
                .setUnits(1).build());
        if (isPlayerModeTWOPLAYERS) {
            valueHead.add(ActivationExt.tanhBlock());
        }

        SequentialBlockExt entropyValueHead = new SequentialBlockExt();
        entropyValueHead.add(Conv1x1LayerNormRelu.builder().channels(1).build())
                .add(BlocksExt.batchFlattenBlock());
        entropyValueHead.add(LinearExt.builder()
                        .setUnits(numChannels) // config.getNumChannels())  // originally 256
                        .build())
                .add(ActivationExt.reluBlock());
        entropyValueHead.add(LinearExt.builder()
                .setUnits(1).build());

        SequentialBlockExt policyHead = new SequentialBlockExt();
        policyHead
                .add(Conv1x1LayerNormRelu.builder().channels(2).build())
                .add(BlocksExt.batchFlattenBlock());
        policyHead.add(LinearExt.builder()
                .setUnits(actionSpaceSize)
                .build());


        add(new ParallelBlockWithCollectChannelJoinExt(
                Arrays.asList(policyHead, valueHead, entropyValueHead))
        );
    }


}
