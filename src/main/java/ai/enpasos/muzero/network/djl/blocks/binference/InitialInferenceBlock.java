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

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.network.djl.blocks.cmainfunctions.PredictionBlock;
import ai.enpasos.muzero.network.djl.blocks.cmainfunctions.RepresentationBlock;
import org.apache.commons.lang3.ArrayUtils;


public class InitialInferenceBlock extends AbstractBlock {

    private static final byte VERSION = 2;
    private final MuZeroConfig config;
    private RepresentationBlock h;
    private PredictionBlock f;


    public InitialInferenceBlock(MuZeroConfig config) {
        super(VERSION);
        this.config = config;

        h = this.addChildBlock("Representation", new RepresentationBlock(config));
        f = this.addChildBlock("Prediction", new PredictionBlock(config));
    }


    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {
        NDList hResult = h.forward(parameterStore, inputs, training, params);
        NDList fResult = f.forward(parameterStore, hResult, training, params);
        return hResult.addAll(fResult);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] hOutputShapes = h.getOutputShapes(inputShapes);
        Shape[] fOutputShapes = f.getOutputShapes(hOutputShapes);
        return ArrayUtils.addAll(hOutputShapes, fOutputShapes);
    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        h.initialize(manager, dataType, inputShapes);
        Shape[] hOutputShapes = h.getOutputShapes(inputShapes);
        f.initialize(manager, dataType, hOutputShapes);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("\nInitialInference(\n");
        for (Block block : children.values()) {
            String blockString = block.toString().replaceAll("(?m)^", "\t");
            sb.append(blockString).append('\n');
        }
        sb.append(')');
        return sb.toString().replace("\t", "  ");
    }


}
