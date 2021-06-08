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

package ai.enpasos.muzero.agent.fast.model.djl.blocks.cmainfunctions;

import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Pipeline;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.enpasos.muzero.agent.fast.model.NetworkIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PredictionListTranslator implements Translator<NetworkIO, List<NetworkIO>> {
    @Override
    public @Nullable Batchifier getBatchifier() {
        return null;
    }

    @Override
    public void prepare(NDManager manager, Model model) {

    }

    @Override
    public List<NetworkIO> processOutput(TranslatorContext ctx, @NotNull NDList list) {


        // moves the data from gpu to cpu


        NDArray p = list.get(0).softmax(1);
        int actionSpaceSize = (int) p.getShape().get(1);
        NDArray v = list.get(1);

        float[] pArray = p.toFloatArray();
        float[] vArray = v.toFloatArray();


        int n = (int) v.getShape().get(0);

        return IntStream.range(0, n)
                .mapToObj(i ->
                {
                    float[] ps = new float[actionSpaceSize];
                    System.arraycopy(pArray, i * actionSpaceSize, ps, 0, actionSpaceSize);
                    return NetworkIO.builder()
                            .value(vArray[i])
                            .policyValues(ps)
                            .build();

                })
                .collect(Collectors.toList());

    }

    @Override
    public @Nullable Pipeline getPipeline() {
        return null;
    }

    @Override
    public @NotNull NDList processInput(TranslatorContext ctx, @NotNull NetworkIO input) {
        return new NDList(input.getHiddenState());
    }


}
