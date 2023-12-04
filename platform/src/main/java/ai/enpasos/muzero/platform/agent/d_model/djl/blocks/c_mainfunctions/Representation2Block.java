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
public class Representation2Block extends MySequentialBlock implements OnnxIO {

    public Representation2Block() {
        super();
    }

    @Builder()
    public static @NotNull Representation2Block newRepresentation2Block(MuZeroConfig config) {
        Representation2Block block = new Representation2Block();

        block
            .add(Conv3x3.builder().channels(config.getNumChannels2()).build())
            .add(new MainRepresentationOrDynamicsBlock( config.getBoardHeight(), config.getBoardWidth(), config.getNumResiduals2(), config.getNumChannels2(), config.getNumBottleneckChannels2(), config.getBroadcastEveryN2()));

        return block;

    }

}
