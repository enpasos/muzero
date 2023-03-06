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

package ai.enpasos.muzero.platform.agent.c_model.djl.blocks.a_training;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.muzero.platform.agent.c_model.djl.blocks.c_mainfunctions.DynamicsBlock;
import ai.enpasos.muzero.platform.agent.c_model.djl.blocks.c_mainfunctions.PredictionBlock;
import ai.enpasos.muzero.platform.agent.c_model.djl.blocks.c_mainfunctions.RepresentationBlock;
import ai.enpasos.muzero.platform.agent.c_model.djl.blocks.c_mainfunctions.SimilarityPredictorBlock;
import ai.enpasos.muzero.platform.agent.c_model.djl.blocks.c_mainfunctions.SimilarityProjectorBlock;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static ai.enpasos.muzero.platform.agent.c_model.djl.blocks.c_mainfunctions.DynamicsBlock.newDynamicsBlock;
import static ai.enpasos.muzero.platform.agent.c_model.djl.blocks.c_mainfunctions.RepresentationBlock.newRepresentationBlock;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class MuZeroBlock extends AbstractBlock {

    private final RepresentationBlock representationBlock;
    private final PredictionBlock predictionBlock;
    private final DynamicsBlock dynamicsBlock;

    private final MuZeroConfig config;
    private final SimilarityPredictorBlock similarityPredictorBlock;
    private final SimilarityProjectorBlock similarityProjectorBlock;

    public MuZeroBlock(MuZeroConfig config) {
        super(MYVERSION);
        this.config = config;


        representationBlock = this.addChildBlock("Representation", newRepresentationBlock(config));
        predictionBlock = this.addChildBlock("Prediction", new PredictionBlock(config));
        dynamicsBlock = this.addChildBlock("Dynamics", newDynamicsBlock(config));

        similarityProjectorBlock = this.addChildBlock("Projector", SimilarityProjectorBlock.newProjectorBlock(
            config.getNumChannelsHiddenLayerSimilarityProjector(), config.getNumChannelsOutputLayerSimilarityProjector()));
        similarityPredictorBlock = this.addChildBlock("Predictor", SimilarityPredictorBlock.newPredictorBlock(
            config.getNumChannelsHiddenLayerSimilarityPredictor(), config.getNumChannelsOutputLayerSimilarityPredictor()));


        inputNames = new ArrayList<>();
        inputNames.add("observation");
        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {
            inputNames.add("action_" + k);
        }
    }


    @Override
    protected @NotNull NDList forwardInternal(@NotNull ParameterStore parameterStore, @NotNull NDList inputs, boolean training, PairList<String, Object> params) {
        NDList combinedResult = new NDList();

        // initial Inference
        NDList representationResult = representationBlock.forward(parameterStore, new NDList(inputs.get(0)), training, params);
        NDArray state = representationResult.get(0);

        NDList predictionResult = predictionBlock.forward(parameterStore, representationResult, training, params);
        combinedResult.add(predictionResult.get(0));
        combinedResult.add(predictionResult.get(1));


        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {


            // recurrent Inference
            NDArray action = inputs.get(2 * k - 1);
            NDList dynamicIn = new NDList(state, action);

            NDList dynamicsResult = dynamicsBlock.forward(parameterStore, dynamicIn, training, params);

            state = dynamicsResult.get(0);

            state = state.scaleGradient(0.5);

            predictionResult = predictionBlock.forward(parameterStore, dynamicsResult, training, params);

            NDList similarityProjectorResultList = this.similarityProjectorBlock.forward(parameterStore, new NDList(state), training, params);
            NDArray similarityPredictorResult = this.similarityPredictorBlock.forward(parameterStore, similarityProjectorResultList, training, params).get(0);

            // initial Inference
            representationResult = representationBlock.forward(parameterStore, new NDList(inputs.get(2 * k)), training, params);
            NDArray similarityProjectorResultLabel = this.similarityProjectorBlock.forward(parameterStore, representationResult, training, params).get(0);
            similarityProjectorResultLabel = similarityProjectorResultLabel.stopGradient();

            combinedResult.add(predictionResult.get(0));
            combinedResult.add(predictionResult.get(1));
            combinedResult.add(similarityPredictorResult);
            combinedResult.add(similarityProjectorResultLabel);

        }
        return combinedResult;
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] outputShapes = new Shape[0];

        // initial Inference
        Shape[] stateOutputShapes = representationBlock.getOutputShapes(new Shape[]{inputShapes[0]});
        outputShapes = ArrayUtils.addAll(outputShapes, predictionBlock.getOutputShapes(stateOutputShapes));

        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {
            // recurrent Inference
            Shape stateShape = stateOutputShapes[0];
            Shape actionShape = inputShapes[k];
            Shape dynamicsInputShape = null;
            if (stateShape.dimension() == 4) {
                dynamicsInputShape = new Shape(stateShape.get(0), stateShape.get(1) + actionShape.get(1), stateShape.get(2), stateShape.get(3));
            } else if (stateShape.size() == 2) {
                dynamicsInputShape = new Shape(stateShape.get(0), stateShape.get(1) + actionShape.get(1));
            } else {
                throw new MuZeroException("wrong input shape");
            }
            stateOutputShapes = dynamicsBlock.getOutputShapes(new Shape[]{dynamicsInputShape});
            outputShapes = ArrayUtils.addAll(outputShapes, predictionBlock.getOutputShapes(stateOutputShapes));

        }
        return outputShapes;
    }


    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("MuZero(\n");
        for (Block block : children.values()) {
            String blockString = block.toString().replaceAll("(?m)^", "\t");
            sb.append(blockString).append('\n');
        }
        sb.append(')');
        return sb.toString();
    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        representationBlock.initialize(manager, dataType, inputShapes[0]);

        Shape[] stateOutputShapes = representationBlock.getOutputShapes(new Shape[]{inputShapes[0]});
        similarityProjectorBlock.initialize(manager, dataType, stateOutputShapes[0]);

        Shape[] projectorOutputShapes = similarityProjectorBlock.getOutputShapes(new Shape[]{stateOutputShapes[0]});
        this.similarityPredictorBlock.initialize(manager, dataType, projectorOutputShapes[0]);

        predictionBlock.initialize(manager, dataType, stateOutputShapes[0]);

        Shape stateShape = stateOutputShapes[0];
        Shape actionShape = inputShapes[1];

        dynamicsBlock.initialize(manager, dataType, stateShape, actionShape);
    }


}
