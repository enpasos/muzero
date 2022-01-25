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

import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.enpasos.muzero.platform.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.platform.agent.fast.model.Observation;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class SelfCriticalTranslator implements Translator<List<SelfCriticalLabeledFeature>, List<Float>> {


    @Override
    public @Nullable Batchifier getBatchifier() {
        return null;
    }

    @Override
    public List<Float> processOutput(TranslatorContext ctx, @NotNull NDList list) {

        List<Float> result = new ArrayList<>();
        long length = list.get(0).getShape().get(0);
        NDArray softmaxed = list.get(0).softmax(1);
        for (int i = 0; i < length; i++) {
            result.add(softmaxed.get(i).toFloatArray()[0]);
        }
        return result;
    }

    @Override
    public @NotNull NDList processInput(@NotNull TranslatorContext ctx, @NotNull List<SelfCriticalLabeledFeature> featureList) {

        int length = featureList.size();
        float[] rawData = new float[length * 3];
      //  float[] rawData = new float[length];
        for (int i = 0; i < length; i++) {
            SelfCriticalLabeledFeature feature = featureList.get(i);
         //   rawData[i] = (float) (feature.entropy);
            rawData[3 * i + 0] = (float) (feature.entropy);
            rawData[3 * i + 1] = (float) feature.normalizedNumberOfMovesPlayedSofar;
            rawData[3 * i + 2] = (float) (feature.toPlayNormalized);
        }

        NDArray[]  data = new NDArray[]{ctx.getNDManager().create(rawData, new Shape(length, 3))};


        return new NDList(data);
    }


}
