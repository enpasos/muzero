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

package ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.binference;

import ai.djl.Device;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.enpasos.muzero.platform.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.platform.agent.fast.model.Observation;
import ai.enpasos.muzero.platform.agent.fast.model.djl.SubModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.MuZeroConfig.hiddenStateRemainOnGPU;

public class InitialInferenceListTranslator implements Translator<List<Observation>, List<NetworkIO>> {
    @Override
    public @Nullable Batchifier getBatchifier() {
        return null;
    }


    @Override
    public List<NetworkIO> processOutput(TranslatorContext ctx, @NotNull NDList list) {


        // intermediate
        NDArray s = list.get(0);


        NDArray hiddenStates = null;
        if (hiddenStateRemainOnGPU || ctx.getNDManager().getDevice().equals(Device.cpu())) {
            hiddenStates = s; //s.toDevice(Device.cpu(), false);
            hiddenStates.detach();
            SubModel submodel = (SubModel) ctx.getModel();
            hiddenStates.attach(submodel.hiddenStateNDManager);
        } else {
            hiddenStates = s.toDevice(Device.cpu(), false);
            s.close();
            hiddenStates.detach();
            SubModel submodel = (SubModel) ctx.getModel();
            hiddenStates.attach(submodel.hiddenStateNDManager);
        }


        NetworkIO outputA = NetworkIO.builder()
                .hiddenState(hiddenStates)
                .build();


        NDArray p = list.get(1).softmax(1);
        int actionSpaceSize = (int) p.getShape().get(1);
        NDArray v = list.get(2);

        float[] pArray = p.toFloatArray();
        float[] vArray = v.toFloatArray();


        int n = (int) v.getShape().get(0);

        List<NetworkIO> networkIOs = IntStream.range(0, n)
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


        for (int i = 0; i < Objects.requireNonNull(networkIOs).size(); i++) {
            networkIOs.get(i).setHiddenState(Objects.requireNonNull(outputA).getHiddenState().get(i));
        }
        return networkIOs;

    }

    @Override
    public @NotNull NDList processInput(@NotNull TranslatorContext ctx, @NotNull List<Observation> inputList) {

        return new NDList(NDArrays.stack(new NDList(
                inputList.stream().map(input -> input.getNDArray(ctx.getNDManager())).collect(Collectors.toList())
        )));
    }


}
