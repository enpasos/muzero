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

package ai.enpasos.muzero.agent.fast.model.djl.blocks.binference;

import ai.djl.Device;
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
import ai.enpasos.muzero.agent.fast.model.djl.SubModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RecurrentInferenceListTranslator implements Translator<NetworkIO, List<NetworkIO>> {
    @Override
    public @Nullable Batchifier getBatchifier() {
        return null;
    }

    @Override
    public void prepare(NDManager manager, Model model) {

    }

    @Override
    public  List<NetworkIO> processOutput(TranslatorContext ctx, @NotNull NDList list) {

        NDArray s = list.get(0);


        NDArray hiddenStates = null;
        if (ctx.getNDManager().getDevice().equals(Device.gpu())) {
            hiddenStates = s; //s.toDevice(Device.cpu(), false);
            hiddenStates.detach();
            SubModel submodel = (SubModel)ctx.getModel();
            hiddenStates.attach(submodel.hiddenStateNDManager);
        } else {
            hiddenStates = s;
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
    public @Nullable Pipeline getPipeline() {
        return null;
    }

    @Override
    public @NotNull NDList processInput(TranslatorContext ctx, @NotNull NetworkIO input) {



        NDArray ndArrayActionStack = NDArrays.stack(new NDList(input.getActionList()));  // on gpu

        NDArray hiddenStateOnTargetDevice = input.getHiddenState();
//        if (ctx.getNDManager().getDevice().equals(Device.gpu())) {
//            hiddenStateOnTargetDevice = input.getHiddenState().toDevice(Device.gpu(), true);
//            hiddenStateOnTargetDevice.attach(ctx.getNDManager());
//        } else {
//            hiddenStateOnTargetDevice = input.getHiddenState();
//        }

        return new NDList(hiddenStateOnTargetDevice, ndArrayActionStack );

    }


}
