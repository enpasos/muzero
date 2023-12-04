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
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.Conv3x3;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
public class Representation1Block extends MySequentialBlock implements OnnxIO {

    public Representation1Block() {
        super();
    }

    @Builder()
    public static @NotNull Representation1Block newRepresentation1Block(MuZeroConfig config) {
        Representation1Block block = new Representation1Block();

        block
            .add(Conv3x3.builder().channels(config.getNumChannels1()).build())
            .add(new MainRepresentationOrDynamicsBlock(config.getBoardHeight(), config.getBoardWidth(), config.getNumResiduals1(), config.getNumChannels1(), config.getNumBottleneckChannels1(), config.getBroadcastEveryN1()));

        return block;

    }

}
