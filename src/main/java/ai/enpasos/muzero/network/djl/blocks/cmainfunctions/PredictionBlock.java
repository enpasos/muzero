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

package ai.enpasos.muzero.network.djl.blocks.cmainfunctions;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.nn.ParallelBlock;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.network.djl.blocks.dlowerlevel.Conv1x1BatchNormRelu;
import ai.enpasos.muzero.network.djl.blocks.dlowerlevel.MySequentialBlock;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PredictionBlock extends MySequentialBlock {


    public PredictionBlock(@NotNull MuZeroConfig config) {


        SequentialBlock valueHead = new SequentialBlock()
                .add(Conv1x1BatchNormRelu.builder().channels(1).build())
                .add(Blocks.batchFlattenBlock())
                .add(Linear.builder()
                        .setUnits(config.getNumChannels()) // config.getNumChannels())  // originally 256
                        .build())
                .add(Activation::relu)
                .add(Linear.builder()
                        .setUnits(1)
                        .build())
                .add(Activation::tanh);

        SequentialBlock policyHead = new SequentialBlock()
                .add(Conv1x1BatchNormRelu.builder().channels(2).build())
                .add(Blocks.batchFlattenBlock())
                .add(Linear.builder()
                        .setUnits(config.getActionSpaceSize())
                        .build());


        add(new ParallelBlock(
                list -> {
                    List<NDArray> concatenatedList = list
                            .stream()
                            .map(NDList::head)
                            .collect(Collectors.toList());

                    return new NDList(concatenatedList);
                }, Arrays.asList(policyHead, valueHead))
        );
    }


}
