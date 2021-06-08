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
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Pipeline;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.enpasos.muzero.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.agent.fast.model.Observation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class RepresentationListTranslator implements Translator<List<Observation>, NetworkIO> {
    @Override
    public @Nullable Batchifier getBatchifier() {
        return null;
    }

    @Override
    public void prepare(NDManager manager, Model model) {

    }

    @Override
    public NetworkIO processOutput(TranslatorContext ctx, @NotNull NDList list) {

        NDArray s = list.get(0);
        NDManager manager = s.getManager().getParentManager().getParentManager();

        s.detach();
        s.attach(manager);

        return NetworkIO.builder()
                .hiddenState(s)
                .build();
    }

    @Override
    public @Nullable Pipeline getPipeline() {
        return null;
    }

    @Override
    public @NotNull NDList processInput(@NotNull TranslatorContext ctx, @NotNull List<Observation> inputList) {


        // here we leave the hiddenState on the device for later processing within the full model

        return new NDList(NDArrays.stack(new NDList(
                inputList.stream().map(input -> input.getNDArray(ctx.getNDManager())).collect(Collectors.toList())
        )));
    }


}
