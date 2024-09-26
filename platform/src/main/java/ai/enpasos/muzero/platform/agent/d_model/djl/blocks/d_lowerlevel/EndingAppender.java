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

package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel;

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.*;
import ai.enpasos.mnist.blocks.ext.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.Conv1x1.newConv1x1;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


@SuppressWarnings("java:S110")
public class EndingAppender extends SequentialBlockExt {

private EndingAppender() {
}

    public static EndingAppender newEndingAppender(Block wrappedBlock, int numCompressedChannels) {
        return (EndingAppender) new EndingAppender()
                .add(wrappedBlock)
                .add(new ParallelBlockWithCollectChannelJoinExt(
                        Arrays.asList(
                                BlocksExt.identityBlock(),
                                (SequentialBlockExt) new SequentialBlockExt()
        // .add(newConv1x1(numCompressedChannels))
                                        .add(new RescaleBlockExt()))));
    }


}
