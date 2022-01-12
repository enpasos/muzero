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

package ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel;

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.*;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.norm.LayerNorm;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;

public class ResidualBlockV2 extends AbstractBlock {


    public final ParallelBlock block;

    public ResidualBlockV2(int numChannels, int squeezeChannelRatio) {
        super(MYVERSION);

        SequentialBlock b1;
        SequentialBlock identity;

        b1 = new SequentialBlock()
                .add(LayerNorm.builder().build())
                .add(Activation::relu)
                .add(Conv2d.builder()
                        .setFilters(numChannels)
                        .setKernelShape(new Shape(3, 3))
                        .optPadding(new Shape(1, 1))
                        .optBias(false)
                        .build())
                .add(LayerNorm.builder().build())
                .add(Activation::relu)
                .add(Conv2d.builder()
                        .setFilters(numChannels)
                        .setKernelShape(new Shape(3, 3))
                        .optPadding(new Shape(1, 1))
                        .optBias(false)
                        .build())
                .add(new SE(numChannels, squeezeChannelRatio))   // Squeeze-and-Excitation Networks
        ;

        identity = new SequentialBlock()
                .add(Blocks.identityBlock());


        block = addChildBlock("residualBlock", new ParallelBlock(
                list -> {
                    NDList unit = list.get(0);
                    NDList parallel = list.get(1);
                    return new NDList(
                            unit.singletonOrThrow()
                                    .add(parallel.singletonOrThrow())
                    );
                },
                Arrays.asList(b1, identity)));
    }

    @Override
    public @NotNull String toString() {
        return "Residual()";
    }

    @Override
    public NDList forward(@NotNull ParameterStore parameterStore, NDList inputs, boolean training) {
        return forward(parameterStore, inputs, training, null);
    }

    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> pairList) {
        return block.forward(parameterStore, inputs, training);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        Shape[] current = inputs;
        for (Block myblock : block.getChildren().values()) {
            current = myblock.getOutputShapes(current);
        }
        return current;
    }

    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        block.initialize(manager, dataType, inputShapes);
    }
}
