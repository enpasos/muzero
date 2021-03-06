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

import ai.enpasos.mnist.blocks.ext.*;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel.Conv1x1LayerNormRelu;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.config.ValueHeadType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@SuppressWarnings("java:S110")
public class PredictionBlock extends MySequentialBlock {

    public PredictionBlock(@NotNull MuZeroConfig config) {
        this(config.getValues().length, config.getNumChannels(), config.getPlayerMode() == PlayerMode.TWO_PLAYERS, config.getActionSpaceSize(), config.getValueHeadType());
    }

    public PredictionBlock(int numCategories, int numChannels, boolean isPlayerModeTWOPLAYERS, int actionSpaceSize, ValueHeadType valueHeadType) {

        SequentialBlockExt valueHead = (SequentialBlockExt) new SequentialBlockExt()
            .add(Conv1x1LayerNormRelu.builder().channels(1).build())
            .add(BlocksExt.batchFlattenBlock())
            .add(LinearExt.builder()
                .setUnits(numChannels) // config.getNumChannels())  // originally 256
                .build())
            .add(ActivationExt.reluBlock());

        if (valueHeadType == ValueHeadType.DISTRIBUTION) {
            valueHead.add(LinearExt.builder()
                .setUnits(numCategories).build());
        } else { // default is EXPECTED
            valueHead.add(LinearExt.builder()
                .setUnits(1).build());


            if (isPlayerModeTWOPLAYERS) {
                valueHead.add(ActivationExt.tanhBlock());
            }
        }

        SequentialBlockExt policyHead = (SequentialBlockExt) new SequentialBlockExt()
            .add(Conv1x1LayerNormRelu.builder().channels(2).build())
            .add(BlocksExt.batchFlattenBlock())
            .add(LinearExt.builder()
                .setUnits(actionSpaceSize)
                .build());


        add(new ParallelBlockWithCollectChannelJoinExt(
            Arrays.asList(policyHead, valueHead))
        );
    }


}
