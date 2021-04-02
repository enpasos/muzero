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

package ai.enpasos.muzero.network.djl.blocks.cmainfunctions;

import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Pipeline;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.enpasos.muzero.network.NetworkIO;

import java.io.IOException;

public class DynamicsListTranslator implements Translator<NetworkIO, NetworkIO> {
    @Override
    public Batchifier getBatchifier() {
        return null;
    }

    @Override
    public void prepare(NDManager manager, Model model) throws IOException {

    }

    @Override
    public NetworkIO processOutput(TranslatorContext ctx, NDList list) throws Exception {

        NDArray s = list.get(0);

        return NetworkIO.builder()
                .hiddenState(s)
                .build();
    }

    @Override
    public Pipeline getPipeline() {
        return null;
    }

    @Override
    public NDList processInput(TranslatorContext ctx, NetworkIO input) throws Exception {

        NDArray ndArrayActionStack = NDArrays.stack(new NDList(input.getActionList()));
        NDArray result = NDArrays.concat(new NDList(input.getHiddenState(), ndArrayActionStack), 1);

        return new NDList(result);

    }


}
