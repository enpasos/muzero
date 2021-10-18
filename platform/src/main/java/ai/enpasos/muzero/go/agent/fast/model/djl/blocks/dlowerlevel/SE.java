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

package ai.enpasos.muzero.go.agent.fast.model.djl.blocks.dlowerlevel;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.*;
import ai.djl.nn.core.Linear;
import ai.djl.nn.pooling.Pool;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class SE extends AbstractBlock {

    private static final byte VERSION = 1;

    public final ParallelBlock block;




    public SE(int numChannels) {
        super(VERSION);

        SequentialBlock b1;
        SequentialBlock identity;

        ParallelBlock globalBlock =
            new ParallelBlock(
                list -> {
                    List<NDArray> concatenatedList =
                            list.stream().map(NDList::head).collect(Collectors.toList());
                    return new NDList(NDArrays.concat(new NDList(concatenatedList), 1));
                },
                Arrays.asList(
                        Pool.globalAvgPool2dBlock(),
                        Pool.globalMaxPool2dBlock()));

        b1 = new SequentialBlock()

                .add(globalBlock)

                .add(Linear.builder()
                        .setUnits(numChannels)
                        .build())
                .add(Activation::relu)
                .add(Linear.builder()
                        .setUnits(numChannels)
                        .build())
                .add(Activation::sigmoid)
        ;

        identity = new SequentialBlock()
                .add(Blocks.identityBlock());




        block = addChildBlock("seBlock", new ParallelBlock(
                list -> {
                    NDArray scaler = list.get(0).singletonOrThrow();
                    Shape newShape = scaler.getShape().add(1,1);
                    scaler = scaler.reshape(newShape);
                    NDArray original = list.get(1).singletonOrThrow();
                    return new NDList(
                            original.mul(scaler)
                    );
                },
                Arrays.asList(b1, identity)));
    }

    @Override
    public @NotNull String toString() {
        return "Residual()";
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
//        Shape[] current = inputs;
//        for (Block block : block.getChildren().values()) {
//            current = block.getOutputShapes(current);
//        }
//        return current;
        return inputs;
    }

    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        block.initialize(manager, dataType, inputShapes);
    }


//    public static void main(String[] args) {
//        NDManager manager = NDManager.newBaseManager();
//        NDArray original = manager.ones(new Shape(1, 2, 3, 4));
//        NDArray scaler = manager.ones(new Shape(1, 2));
//        Shape newShape = scaler.getShape().add(1,1);
//        scaler = scaler.reshape(newShape);
//        NDArray prod = original.mul(scaler);
//        int i = 42;
//    }
}
