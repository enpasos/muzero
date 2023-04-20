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

package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.nn.Block;
import ai.enpasos.muzero.platform.agent.c_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Slf4j
@Component
public class ParameterNames {

    @Autowired
    MuZeroConfig config;

    public String listParameterNames() {

        StringBuilder buf = new StringBuilder();
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {

            if (model.getBlock() == null) {
                Block block = new MuZeroBlock(config);
                model.setBlock(block);
                try {
                    model.load(Paths.get(config.getNetworkBaseDir()));
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
