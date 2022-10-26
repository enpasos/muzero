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

import ai.enpasos.mnist.blocks.ext.ActivationExt;
import ai.enpasos.mnist.blocks.ext.LayerNormExt;
import ai.enpasos.mnist.blocks.ext.LinearExt;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel.MySequentialBlock;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
public class SimilarityPredictorBlock extends MySequentialBlock {


        private SimilarityPredictorBlock() {
        }

        @Builder()
        public static @NotNull SimilarityPredictorBlock newPredictorBlock(int hiddenChannels, int outputChannels) {
            SimilarityPredictorBlock instance = new SimilarityPredictorBlock();
            instance

                // layer 1
                .add(LinearExt.builder()
                    .setUnits(hiddenChannels)
                    .build())
                .add(LayerNormExt.builder().build())
                .add(ActivationExt.reluBlock())

                // layer 2
                .add(LinearExt.builder()
                    .setUnits(outputChannels)
                    .build())
                .add(LayerNormExt.builder().build());

            return instance;
        }


    }
