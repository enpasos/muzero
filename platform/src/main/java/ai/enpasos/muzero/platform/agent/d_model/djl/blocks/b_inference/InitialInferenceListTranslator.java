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

package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference;

import ai.djl.Device;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyL2Loss;
import ai.enpasos.muzero.platform.agent.d_model.djl.SubModel;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class InitialInferenceListTranslator implements Translator<List<Game>, List<NetworkIO>> {
    public static List<NetworkIO> getNetworkIOS(@NotNull NDList list, TranslatorContext ctx) {
        NDArray hiddenStates;
        NDArray s = list.get(0);
        SubModel submodel = (SubModel) ctx.getModel();
        MuZeroConfig config = submodel.getConfig();
        if (MuZeroConfig.HIDDEN_STATE_REMAIN_ON_GPU || ctx.getNDManager().getDevice().equals(Device.cpu())) {
            hiddenStates = s;
            hiddenStates.attach(submodel.getHiddenStateNDManager());
        } else {
            hiddenStates = s.toDevice(Device.cpu(), false);
            NDManager hiddenStateNDManager = hiddenStates.getManager();
            hiddenStates.attach(submodel.getHiddenStateNDManager());
            hiddenStateNDManager.close();
            s.close();
        }

        NetworkIO outputA = NetworkIO.builder()
            .hiddenState(hiddenStates)
            .build();


        NDArray logits = list.get(1);

        NDArray p = logits.softmax(1);

        int actionSpaceSize = (int) logits.getShape().get(1);


        float[] logitsArray = logits.toFloatArray();

        float[] pArray = p.toFloatArray();

        NDArray v = list.get(2);
        float[] vArray = v.toFloatArray();


        NDArray vEntropy = list.get(3);
        float[] vEntropyArray = vEntropy.toFloatArray();

        int n = (int) v.getShape().get(0);

        List<NetworkIO> networkIOs = IntStream.range(0, n)
            .mapToObj(i ->
            {
                float[] ps = new float[actionSpaceSize];

                float[] logits2 = new float[actionSpaceSize];
                System.arraycopy(logitsArray, i * actionSpaceSize, logits2, 0, actionSpaceSize);
                System.arraycopy(pArray, i * actionSpaceSize, ps, 0, actionSpaceSize);

                double scale = config.getValueSpan() / 2.0;
                return NetworkIO.builder()
                    .value(vArray[i] == MyL2Loss.NULL_VALUE ? MyL2Loss.NULL_VALUE : scale * vArray[i])
                    .entropyValue(vEntropyArray[i])
                    .policyValues(ps)
                    .logits(logits2)
                    .build();


            })
            .collect(Collectors.toList());


        for (int i = 0; i < Objects.requireNonNull(networkIOs).size(); i++) {
            networkIOs.get(i).setHiddenState(Objects.requireNonNull(outputA).getHiddenState().get(i));
        }
        hiddenStates.close();
        return networkIOs;
    }

    @Override
    public @Nullable Batchifier getBatchifier() {
        return null;
    }

    @Override
    public List<NetworkIO> processOutput(TranslatorContext ctx, @NotNull NDList list) {
        return getNetworkIOS(list, ctx);
    }

    @Override
    public @NotNull NDList processInput(@NotNull TranslatorContext ctx, @NotNull List<Game> gameList) {


        List<ObservationModelInput> observations = gameList.stream()
            .map(Game::getObservationModelInput)
            .collect(Collectors.toList());

        return new NDList(NDArrays.stack(new NDList(
            observations.stream().map(input -> input.getNDArray(ctx.getNDManager())).collect(Collectors.toList())
        )));
    }


}
