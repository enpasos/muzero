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

package ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference;

import ai.djl.Device;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.intuitive.Observation;
import ai.enpasos.muzero.platform.agent.intuitive.djl.SubModel;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.ValueConverter;
import ai.enpasos.muzero.platform.config.ValueHeadType;
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

        NDArray vp = null;
        float[]  vpArray = null;
        if (config.getValueHeadType() == ValueHeadType.DISTRIBUTION) {
            vp =  v.softmax(1);
            vpArray = vp.toFloatArray();
        }
        final float[]  vpArrayFinal = vpArray;

        int n = (int) v.getShape().get(0);

        List<NetworkIO> networkIOs = IntStream.range(0, n)
            .mapToObj(i ->
            {
                float[] ps = new float[actionSpaceSize];

                float[] logits2 = new float[actionSpaceSize];
                System.arraycopy(logitsArray, i * actionSpaceSize, logits2, 0, actionSpaceSize);
                System.arraycopy(pArray, i * actionSpaceSize, ps, 0, actionSpaceSize);
                if (config.getValueHeadType() == ValueHeadType.DISTRIBUTION) {
                    int l = config.getValues().length;
                    float[] vps = new float[l];
                    System.arraycopy(vpArrayFinal, i * l, vps, 0, l);
                    return NetworkIO.builder()
                        .value(ValueConverter.expectedValue(config, vps))
                        .valueDistribution(vps)
                        .policyValues(ps)
                        .logits(logits2)
                        .build();
                } else {
                    return NetworkIO.builder()
                        .value(vArray[i])
                        .policyValues(ps)
                        .logits(logits2)
                        .build();
                }


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


        List<Observation> observations = gameList.stream()
            .map(g -> g.getObservation(ctx.getNDManager()))
            .collect(Collectors.toList());

        return new NDList(NDArrays.stack(new NDList(
            observations.stream().map(input -> input.getNDArray(ctx.getNDManager())).collect(Collectors.toList())
        )));
    }


}
