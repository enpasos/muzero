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

package ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.ext.ActivationExt;
import ai.enpasos.mnist.blocks.ext.Conv2dExt;
import ai.enpasos.mnist.blocks.ext.LayerNormExt;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;


public class Conv1x1LayerNormRelu extends MySequentialBlock {


    private Conv1x1LayerNormRelu() {
    }

    @Builder()
    public static @NotNull Conv1x1LayerNormRelu newConvLayerNormRelu(int channels) {
        Conv1x1LayerNormRelu instance = new Conv1x1LayerNormRelu();
        instance.add(
                Conv2dExt.builder()
                    .setFilters(channels)
                    .setKernelShape(new Shape(1, 1))
                    .optBias(false)
                    .build())
            .add(LayerNormExt.builder().build())
            .add(ActivationExt.reluBlock());
        return instance;
    }


}
