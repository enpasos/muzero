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

package ai.enpasos.muzero.platform.debug;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.nn.Block;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;

import static ai.enpasos.muzero.platform.MuZero.getNetworksBasedir;

@Slf4j
public class ParameterNames {
    public static String listParameterNames(MuZeroConfig config) {

        StringBuffer buf = new StringBuffer();
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {

            Block block = model.getBlock();
            if (model.getBlock() == null) {
                block = new MuZeroBlock(config);
                model.setBlock(block);
                try {
                    model.load(Paths.get(getNetworksBasedir(config)));
                } catch (Exception e) {
                    log.info("*** no existing model has been found ***");
                }

                block.getParameters().forEach(
                        //  p -> System.out.println(p.getKey() + " = " + Arrays.toString(p.getValue().getArray().toFloatArray()))
                        p -> buf.append(p.getKey())
                );
            }
            return buf.toString();
        }
    }
}
