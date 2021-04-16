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

package ai.enpasos.muzero.network.djl.blocks.binference;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.network.djl.blocks.cmainfunctions.DynamicsBlock;
import ai.enpasos.muzero.network.djl.blocks.cmainfunctions.PredictionBlock;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;


public class RecurrentInferenceBlock extends AbstractBlock {

    private static final byte VERSION = 2;
    private final DynamicsBlock g;
    private final PredictionBlock f;


    public RecurrentInferenceBlock(@NotNull MuZeroConfig config) {
        super(VERSION);

        g = this.addChildBlock("Dynamics", new DynamicsBlock(config));
        f = this.addChildBlock("Prediction", new PredictionBlock(config));
    }


    /**
     * @param inputs First input for state, second for action
     */
    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, @NotNull NDList inputs, boolean training, PairList<String, Object> params) {
        NDArray state = inputs.get(0);
        NDArray action = inputs.get(1);
        NDArray inputsAll = NDArrays.concat(new NDList(state, action), 1);


        NDList gResult = g.forward(parameterStore, new NDList(inputsAll), training);
        NDList fResult = f.forward(parameterStore, gResult, training);
        return gResult.addAll(fResult);
    }


    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] gOutputShapes = g.getOutputShapes(inputShapes);
        Shape[] fOutputShapes = f.getOutputShapes(gOutputShapes);
        return ArrayUtils.addAll(gOutputShapes, fOutputShapes);
    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        Shape stateShape = inputShapes[0];
        Shape actionShape = inputShapes[1];
        Shape hInputShapes = new Shape(stateShape.get(0), stateShape.get(1) + actionShape.get(1), stateShape.get(2), stateShape.get(3));

        g.initialize(manager, dataType, hInputShapes);
        Shape[] hOutputShapes = g.getOutputShapes(inputShapes);
        f.initialize(manager, dataType, hOutputShapes);
    }

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("RecurrentInference(\n");
        for (Block block : children.values()) {
            String blockString = block.toString().replaceAll("(?m)^", "\t");
            sb.append(blockString).append('\n');
        }
        sb.append(')');
        return sb.toString().replace("\t", "  ");
    }


}
