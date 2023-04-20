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

package ai.enpasos.muzero.platform.agent.c_model.djl.blocks.d_lowerlevel;

import ai.djl.nn.Block;
import ai.enpasos.mnist.blocks.ext.SequentialBlockExt;
import org.jetbrains.annotations.NotNull;

public class MySequentialBlock extends SequentialBlockExt {

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append(this.getClass().getSimpleName()).append("(\n");
        for (Block block : children.values()) {
            String blockString = block.toString().replaceAll("(?m)^", "\t");
            sb.append(blockString).append('\n');
        }
        sb.append(')');
        return sb.toString();
    }
}
