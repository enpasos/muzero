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

package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions;

import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.CausalityFreezing;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
public class RepresentationBlock extends MySequentialBlock implements OnnxIO, CausalityFreezing {

    private RepresentationBlock() {
        super();
    }

    RepresentationStart representationStart;
    MainRepresentationOrDynamicsBlock mainRepresentationOrDynamicsBlock;

    @Builder()
    public static @NotNull RepresentationBlock newRepresentationBlock(MuZeroConfig config) {
        RepresentationBlock block = new RepresentationBlock();
        block.representationStart = new RepresentationStart(config);
        block.mainRepresentationOrDynamicsBlock = new MainRepresentationOrDynamicsBlock(config );

        block
            .add(block.representationStart)
            .add(block.mainRepresentationOrDynamicsBlock);

        return block;

    }

    @Override
    public void freeze(boolean[] freeze) {
        this.getChildren().forEach(b -> {
            if (b.getValue() instanceof CausalityFreezing) {
                ((CausalityFreezing) b.getValue()).freeze(freeze);
            }
        });
    }

    public RepresentationBlock getBlockForInitialRulesOnly(MuZeroConfig config) {
        RepresentationBlock block2 = new RepresentationBlock();
        block2
                .add(representationStart.getBlockForInitialRulesOnly())
                .add(mainRepresentationOrDynamicsBlock.getBlockForInitialRulesOnly(config));

        return block2;
    }
}
