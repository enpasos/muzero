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
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference.InitialInferenceListTranslator.getNetworkIOS;


public class RecurrentInferenceListTranslator implements Translator<NetworkIO, List<NetworkIO>> {
    @Override
    public @Nullable Batchifier getBatchifier() {
        return null;
    }


    @Override
    public List<NetworkIO> processOutput(TranslatorContext ctx, @NotNull NDList list) {
        return getNetworkIOS(list, ctx);
    }


    @Override
    public @NotNull NDList processInput(TranslatorContext ctx, @NotNull NetworkIO input) {


        NDArray ndArrayActionStack = NDArrays.stack(new NDList(input.getActionList()));  // on gpu

        NDArray hiddenStateOnTargetDevice = input.getHiddenState();
        if (!MuZeroConfig.HIDDEN_STATE_REMAIN_ON_GPU && ctx.getNDManager().getDevice().equals(Device.gpu())) {
            hiddenStateOnTargetDevice = input.getHiddenState().toDevice(Device.gpu(), true);
            hiddenStateOnTargetDevice.attach(ctx.getNDManager());
            ndArrayActionStack.attach(ctx.getNDManager());
            return new NDList(hiddenStateOnTargetDevice, ndArrayActionStack);
        }

        return new NDList(hiddenStateOnTargetDevice, ndArrayActionStack);

    }


}
