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

package ai.enpasos.mnist.blocks;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.ext.*;

import java.util.Arrays;


public class MnistBlock extends SequentialBlockExt implements OnnxIO {
    public static MnistBlock newMnistBlock() {
        return (MnistBlock) new MnistBlock()
                .add(Conv2dExt.builder()
                        .setFilters(8)
                        .setKernelShape(new Shape(5, 5))
                        .optBias(false)
                        .optPadding(new Shape(2, 2))
                        .build())
                .add(LayerNormExt.builder().build())
                .add(ActivationExt.reluBlock())
                .add(PoolExt.maxPool2dBlock(new Shape(2, 2), new Shape(2, 2)))   // 28 -> 14
                .add(
                    new ParallelBlockWithConcatChannelJoinExt(
                        Arrays.asList(
                            Conv2dExt.builder()
                                .setFilters(16)
                                .setKernelShape(new Shape(5, 5))
                                .optBias(false)
                                .optPadding(new Shape(2, 2))
                                .build(),
                            Conv2dExt.builder()
                                .setFilters(16)
                                .setKernelShape(new Shape(3, 3))
                                .optBias(false)
                                .optPadding(new Shape(1, 1))
                                .build()
                        ))
                )
                .add(new SqueezeExciteExt(32, 8))
                .add(LayerNormExt.builder().build())
                .add(ActivationExt.reluBlock())
                .add(PoolExt.maxPool2dBlock(new Shape(2, 2), new Shape(2, 2)))  // 14 -> 7
                .add(Conv2dExt.builder()
                        .setFilters(32)
                        .setKernelShape(new Shape(3, 3))
                        .optBias(false)
                        .optPadding(new Shape(1, 1))
                        .build())
                .add(LayerNormExt.builder().build())
                .add(ActivationExt.reluBlock())
                .add(new RescaleBlockExt())
                .add(BlocksExt.batchFlattenBlock())
                .add(LinearExt.builder()
                        .setUnits(10)
                        .optBias(true)
                        .build());
    }

    private MnistBlock() {}

}
