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
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.mnist.blocks.ext.BlocksExt;
import ai.enpasos.mnist.blocks.ext.ParallelBlockWithAddJoinExt;
import ai.enpasos.mnist.blocks.ext.SequentialBlockExt;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;

public class StartResidualBlock extends AbstractBlock implements OnnxIO {


    public final ParallelBlockWithAddJoinExt block;

    public StartResidualBlock(int numChannels) {
        super(MYVERSION);

        SequentialBlockExt b1;
        SequentialBlockExt identity;

        b1 = (SequentialBlockExt) new SequentialBlockExt()
            .add(new ConcatInputsBlock())
            .add(Conv3x3.builder().channels(numChannels).build())
        ;

        identity = (SequentialBlockExt) new SequentialBlockExt()
            .add(BlocksExt.identityBlock());

        block = addChildBlock("startResidualBlock", new ParallelBlockWithAddJoinExt(
            Arrays.asList(b1, identity)));
    }

    @Override
    public @NotNull String toString() {
        return "StartResidual()";
    }

    @Override
    public NDList forward(@NotNull ParameterStore parameterStore, NDList inputs, boolean training) {
        return forward(parameterStore, inputs, training, null);
    }

    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> pairList) {
        return block.forward(parameterStore, inputs, training);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        List<Shape> shapes = new ArrayList<>();
        for (Block myblock : block.getChildren().values()) {
            shapes.add(myblock.getOutputShapes(inputs)[0]);
        }
        return shapes.toArray(new Shape[0]);
    }

    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        block.initialize(manager, dataType, inputShapes);
    }

    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        return block.getOnnxBlock(counter, input);
    }
}
