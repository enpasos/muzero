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

package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.nn.ParameterList;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.CausalityFreezing;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.*;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.DynamicsBlock.newDynamicsBlock;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.RepresentationBlock.newRepresentationBlock;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class MuZeroBlock extends AbstractBlock implements  CausalityFreezing {

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

        // added predictions to the combinedResult in the following order:
        // initial inference
        // - rules layer: legal actions
        // - policy layer:  policy
        // - value layer:  value
        // each recurrent inference
        // - rules layer: consistency loss
        // - rules layer: reward
        // - rules layer: legal actions
        // - policy layer:  policy
        // - value layer:  value

        // initial Inference
        predictionBlock.setWithReward(false);
        NDList representationResult = representationBlock.forward(parameterStore, new NDList(inputs.get(0)), training, params);
        NDList stateForPrediction = new NDList(representationResult.get(0), representationResult.get(1), representationResult.get(2));
        NDList stateForTimeEvolution = new NDList(representationResult.get(3), representationResult.get(4), representationResult.get(5));

        NDList predictionResult = predictionBlock.forward(parameterStore, stateForPrediction, training, params);
        for (NDArray prediction : predictionResult.getResourceNDArrays()) {
            combinedResult.add(prediction);
        }

        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {


            // recurrent Inference

            predictionBlock.setWithReward(true);

            NDArray action = inputs.get(2 * k - 1);

            NDList dynamicIn = new NDList();
            dynamicIn.addAll(stateForTimeEvolution);
            dynamicIn.add(action);

            NDList dynamicsResult = dynamicsBlock.forward(parameterStore, dynamicIn, training, params);

             stateForPrediction = new NDList(dynamicsResult.get(0), dynamicsResult.get(1), dynamicsResult.get(2));
             stateForTimeEvolution = new NDList(dynamicsResult.get(3), dynamicsResult.get(4), dynamicsResult.get(5));


            predictionResult = predictionBlock.forward(parameterStore, stateForPrediction, training, params);


            // TODO check similarityProjector input and output
            NDList similarityProjectorResultList = this.similarityProjectorBlock.forward(parameterStore, new NDList( stateForTimeEvolution.get(0)), training, params);
            NDArray similarityPredictorResult = this.similarityPredictorBlock.forward(parameterStore, similarityProjectorResultList, training, params).get(0);


            representationResult = representationBlock.forward(parameterStore, new NDList(inputs.get(2 * k)), training, params);
            NDArray similarityProjectorResultLabel = this.similarityProjectorBlock.forward(parameterStore, representationResult, training, params).get(0);
            similarityProjectorResultLabel = similarityProjectorResultLabel.stopGradient();

            combinedResult.add(similarityPredictorResult);
            combinedResult.add(similarityProjectorResultLabel);

            for (NDArray prediction : predictionResult.getResourceNDArrays()) {
                combinedResult.add(prediction);
            }

            NDList temp = new NDList();
            for (int i = 0; i < stateForTimeEvolution.size(); i++) {
                temp.add(stateForTimeEvolution.get(i).scaleGradient(0.5));
            }
            stateForTimeEvolution = temp;
//            temp = new NDList();
//            for (int i = 0; i < stateForPrediction.size(); i++) {
//                temp.add(stateForPrediction.get(i).scaleGradient(0.5));
//            }
//            stateForPrediction = temp;

        }
        return combinedResult;
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] outputShapes = new Shape[0];

        // initial Inference
        predictionBlock.setWithReward(false);
        Shape[] stateOutputShapes = representationBlock.getOutputShapes(new Shape[]{inputShapes[0]});

        Shape[] stateOutputShapesForPrediction = new Shape[]{stateOutputShapes[0], stateOutputShapes[1], stateOutputShapes[2]};
        Shape[] stateOutputShapesForTimeEvolution = new Shape[]{stateOutputShapes[3], stateOutputShapes[4], stateOutputShapes[5]};




        Shape[] predictionBlockOutputShapes = predictionBlock.getOutputShapes(stateOutputShapesForPrediction);
        outputShapes = ArrayUtils.addAll(stateOutputShapesForTimeEvolution, predictionBlockOutputShapes);

        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {
            // recurrent Inference
            predictionBlock.setWithReward(true);
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

            stateOutputShapesForPrediction = new Shape[]{stateOutputShapes[0], stateOutputShapes[1], stateOutputShapes[2]};
             stateOutputShapesForTimeEvolution = new Shape[]{stateOutputShapes[3], stateOutputShapes[4], stateOutputShapes[5]};



            outputShapes = ArrayUtils.addAll(outputShapes, predictionBlock.getOutputShapes(stateOutputShapesForPrediction));

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

        Shape[] predictionInputShape = new Shape[3];
        predictionInputShape[0] = stateOutputShapes[0];
        predictionInputShape[1] = stateOutputShapes[1];
        predictionInputShape[2] = stateOutputShapes[2];



        predictionBlock.setWithReward(true);
        predictionBlock.initialize(manager, dataType, predictionInputShape);


        Shape actionShape = inputShapes[1];
        Shape[] dynamicsInputShape = new Shape[4];
        dynamicsInputShape[0] = stateOutputShapes[3];
        dynamicsInputShape[1] = stateOutputShapes[4];
        dynamicsInputShape[2] = stateOutputShapes[5];
        dynamicsInputShape[3] = actionShape;

        dynamicsBlock.initialize(manager, dataType, dynamicsInputShape);


    }


    @Override
    public void freeze(boolean[] freeze) {
        this.predictionBlock.freeze(freeze);
        this.dynamicsBlock.freeze(freeze);
        this.similarityPredictorBlock.freezeParameters(freeze[0]);
        this.similarityProjectorBlock.freezeParameters(freeze[0]);
        this.representationBlock.freeze(freeze);

    }
}
