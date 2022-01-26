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

package ai.enpasos.muzero.go.selfcritical;

import ai.djl.basicmodelzoo.basic.Mlp;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.enpasos.mnist.blocks.MnistBlock;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.SqueezeExciteExt;
import ai.enpasos.mnist.blocks.ext.*;

import java.util.Arrays;


public class SelfCriticalBlock extends SequentialBlockExt implements OnnxIO {
    public static SelfCriticalBlock newSelfCriticalBlock(int maxFullMoves) {
        return (SelfCriticalBlock) new SelfCriticalBlock()
            .add(Conv2dExt.builder()
                .setFilters(8)
                .setKernelShape(new Shape(2, 5))  // xN -> xN
                .optBias(false)
                .optPadding(new Shape(0, 2))
                .build())
            .add(LayerNormExt.builder().build())
            .add(ActivationExt.reluBlock())
            .add(PoolExt.maxPool2dBlock(new Shape(2, 1), new Shape(2, 1)))   // yN -> 1
            .add(Conv2dExt.builder()
                .setFilters(16)
                .setKernelShape(new Shape(1, 3))
                .optBias(false)
                .optPadding(new Shape(0, 1))
                .build())
            .add(LayerNormExt.builder().build())
            .add(ActivationExt.reluBlock())
            .add(Conv2dExt.builder()
                .setFilters(32)
                .setKernelShape(new Shape(1, 3))
                .optBias(false)
                .optPadding(new Shape(0, 1))
                .build())
            .add(LayerNormExt.builder().build())
            .add(ActivationExt.reluBlock())
            .add(BlocksExt.batchFlattenBlock())
            .add(LinearExt.builder()
                .setUnits(maxFullMoves)
                .optBias(true)
                .build());
    }

    private SelfCriticalBlock() {}

}
