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

package ai.enpasos.mnist.blocks;

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.SequentialBlock;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.ext.*;

import java.util.Arrays;
import java.util.List;


public class SqueezeExciteExt extends AbstractBlock implements OnnxIO {


    public final ParallelBlockWithScalerJoinExt block;


    public SqueezeExciteExt(int numChannels, int squeezeChannelRatio) {
        super((byte) 1);

        SequentialBlock b1;
        SequentialBlock identity;

        ParallelBlockWithConcatChannelJoinExt globalBlock =
            new ParallelBlockWithConcatChannelJoinExt(
                Arrays.asList(
                    PoolExt.globalAvgPool2dBlock(),
                    PoolExt.globalMaxPool2dBlock()));

        b1 = new SequentialBlockExt()
            .add(globalBlock)
            .add(BlocksExt.batchFlattenBlock())  // TODO intermediate workaround for mismatch DJL <-> ONNX
            .add(LinearExt.builder()
                .setUnits(numChannels / squeezeChannelRatio)
                .build())
            .add(ActivationExt.reluBlock())
            .add(LinearExt.builder()
                .setUnits(numChannels)
                .build())
            .add(ActivationExt.sigmoidBlock())

        ;

        identity = new SequentialBlockExt()
            .add(BlocksExt.identityBlock());

        block = addChildBlock("seBlock", new ParallelBlockWithScalerJoinExt(Arrays.asList(b1, identity)));
    }

    @Override
    public String toString() {
        return "Residual()";
    }

    @Override
    public NDList forward(ParameterStore parameterStore, NDList inputs, boolean training) {
        return forward(parameterStore, inputs, training, null);
    }

    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> pairList) {
        return block.forward(parameterStore, inputs, training);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        return inputs;
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
