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
import ai.enpasos.mnist.blocks.ext.ActivationExt;
import ai.enpasos.mnist.blocks.ext.LayerNormExt;
import ai.enpasos.mnist.blocks.ext.LinearExt;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.MySequentialBlock;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
public class SimilarityPredictorBlock extends MySequentialBlock implements DCLAware {

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
                .build());

        return instance;
    }

    @Override
    public void freeze(boolean[] freeze) {
        this.freezeParameters(freeze[0]);
    }
}
