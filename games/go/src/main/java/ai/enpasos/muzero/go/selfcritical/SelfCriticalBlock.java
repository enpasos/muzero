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

package ai.enpasos.muzero.go.selfcritical;

import ai.djl.basicmodelzoo.basic.Mlp;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.SqueezeExciteExt;
import ai.enpasos.mnist.blocks.ext.*;

import java.util.Arrays;


public class SelfCriticalBlock extends SequentialBlockExt implements OnnxIO {
    public static SelfCriticalBlock newSelfCriticalBlock() {
        return (SelfCriticalBlock) new SelfCriticalBlock()
            .add(
                new Mlp(
                3,
                2,
                new int[] {10, 6}));
    }

    private SelfCriticalBlock() {}

}
