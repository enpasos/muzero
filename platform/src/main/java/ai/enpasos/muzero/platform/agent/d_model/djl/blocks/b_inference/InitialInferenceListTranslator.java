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
import ai.djl.util.Pair;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyBCELoss;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyL2Loss;
import ai.enpasos.muzero.platform.agent.d_model.djl.SubModel;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;



public class InitialInferenceListTranslator implements Translator<Pair<List<Game>, List<NDArray>>, List<NetworkIO>> {
    public static List<NetworkIO> getNetworkIOS(@NotNull NDList list, TranslatorContext ctx, boolean isRecurrent) {

        // hiddenstate 0, 1, 2
        // legalActions 3
        // reward 4
        // policy 5
        // value 6



        int offset = 0;
        if (isRecurrent) {
            offset = 1;
        }


        SubModel submodel = (SubModel) ctx.getModel();
        MuZeroConfig config = submodel.getConfig();

        int hN = list.size() - 3 - offset;  //
        NDArray[] hiddenStates = new NDArray[hN];
        for (int i = 0; i < hN; i++) {
            hiddenStates[i] = getHiddenStates(list.get(i), ctx, submodel);
        }


        NetworkIO outputA = NetworkIO.builder()
            .hiddenState(hiddenStates)
            .build();

        // 4   : legalActions
        float[]  pLegalArray_ = MyBCELoss.sigmoid(list.get(hN)).toFloatArray();
        final float[] pLegalArray = pLegalArray_;


        // 4: reward
        NDArray r_ = null;
        float[] rArray_ = null;
        if (isRecurrent) {
            r_ = list.get(hN+1);
            rArray_ = r_.toFloatArray();
        }
        float[] rArray  = rArray_;
        NDArray r = r_;

        // 4 + offset: policy
        NDArray logits = list.get(hN + offset + 1);
        NDArray p = logits.softmax(1);
        int actionSpaceSize = (int) logits.getShape().get(1);
        float[] logitsArray = logits.toFloatArray();
        float[] pArray = p.toFloatArray();



        // 5 + offset: value
        NDArray v = list.get(hN + offset + 2);
        float[] vArray = v.toFloatArray();
        int n = (int) v.getShape().get(0);




        List<NetworkIO> networkIOs = IntStream.range(0, n)
            .mapToObj(i ->
            {
                float[] ps = new float[actionSpaceSize];
                float[] psLegal = null;
                if (pLegalArray != null) {
                    psLegal = new float[actionSpaceSize];
                }

                float[] logits2 = new float[actionSpaceSize];
                System.arraycopy(logitsArray, i * actionSpaceSize, logits2, 0, actionSpaceSize);
                System.arraycopy(pArray, i * actionSpaceSize, ps, 0, actionSpaceSize);
                if (pLegalArray != null) {
                    System.arraycopy(pLegalArray, i * actionSpaceSize, psLegal, 0, actionSpaceSize);
                }
                double scale = config.getValueSpan() / 2.0;

                double reward = isRecurrent ? scale * rArray[i] : 0.0;


                return NetworkIO.builder()
                    .value(vArray[i] == MyL2Loss.NULL_VALUE ? MyL2Loss.NULL_VALUE : scale * vArray[i])
                    .pLegalValues(psLegal)
                    .policyValues(ps)
                        .reward(reward)
                    .logits(logits2)
                    .build();

            })
            .collect(Collectors.toList());


        for (int i = 0; i < Objects.requireNonNull(networkIOs).size(); i++) {
            int nh = outputA.getHiddenState().length;
            networkIOs.get(i).setHiddenState(new NDArray[nh]);
            for (int h = 0; h < nh; h++) {
                networkIOs.get(i).getHiddenState()[h] = outputA.getHiddenState()[h].get(i);
            }
        }
        Arrays.stream(outputA.getHiddenState()).forEach(h -> h.close()) ;
        return networkIOs;
    }

    @NotNull
    private static NDArray getHiddenStates( NDArray s, TranslatorContext ctx, SubModel submodel) {
        NDArray hiddenStates;

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
        return hiddenStates;
    }

    @Override
    public @Nullable Batchifier getBatchifier() {
        return null;
    }

    @Override
    public List<NetworkIO> processOutput(TranslatorContext ctx, @NotNull NDList list) {
        return getNetworkIOS(list, ctx, false);
    }

    @Override
    public @NotNull NDList processInput(@NotNull TranslatorContext ctx, @NotNull Pair<List<Game>, List<NDArray>> input0) {
        List<Game> gameList = input0.getKey();
        List<NDArray> actionList = input0.getValue();



        List<ObservationModelInput> observations = gameList.stream()
            .map(Game::getObservationModelInput)
            .collect(Collectors.toList());


        NDArray ndArrayObservationStack = NDArrays.stack(new NDList(
             observations.stream().map(input -> input.getNDArray(ctx.getNDManager())).collect(Collectors.toList())
         ));


        NDArray ndArrayActionStack = NDArrays.stack(new NDList(actionList));  // on gpu


        return new NDList(ndArrayObservationStack, ndArrayActionStack);
    }


}
