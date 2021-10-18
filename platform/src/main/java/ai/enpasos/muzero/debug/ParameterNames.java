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

package ai.enpasos.muzero.debug;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.MuZero;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;

@Slf4j
public class ParameterNames {
    public static String listParameterNames(MuZeroConfig config) {
        MuZeroBlock block = new MuZeroBlock(config);
        StringBuffer buf = new StringBuffer();
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            model.setBlock(block);


            try {
                model.load(Paths.get(MuZero.getNetworksBasedir(config)));

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage(), e);
            }
            block.getParameters().forEach(
                    //  p -> System.out.println(p.getKey() + " = " + Arrays.toString(p.getValue().getArray().toFloatArray()))
                    p -> buf.append(p.getKey())
            );
            return buf.toString();
        }
    }
}
