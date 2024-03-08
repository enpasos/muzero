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

import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.InitialInferenceBlock.firstHalfNDList;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.InitialInferenceBlock.secondHalfNDList;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.DynamicsBlock.newDynamicsBlock;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.RepresentationBlock.newRepresentationBlock;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class MuZeroBlock extends AbstractBlock implements  CausalityFreezing {

    private final RepresentationBlock representationBlock;
    private final PredictionBlock predictionBlock;
    private final DynamicsBlock dynamicsBlock;

    private final MuZeroConfig config;
    private final SimilarityPredictorBlock similarityPredictorBlock ;
    private final SimilarityProjectorBlock similarityProjectorBlock ;

    public MuZeroBlock(MuZeroConfig config) {
        super(MYVERSION);
        this.config = config;


        representationBlock = this.addChildBlock("Representation", newRepresentationBlock(config));
        predictionBlock = this.addChildBlock("Prediction", new PredictionBlock(config));
        dynamicsBlock = this.addChildBlock("Dynamics", newDynamicsBlock(config));

        if (config.isWithConsistencyLoss()) {
            similarityProjectorBlock = this.addChildBlock("Projector", SimilarityProjectorBlock.newProjectorBlock(
                    config.getNumChannelsHiddenLayerSimilarityProjector(), config.getNumChannelsOutputLayerSimilarityProjector()));
            similarityPredictorBlock = this.addChildBlock("Predictor", SimilarityPredictorBlock.newPredictorBlock(
                    config.getNumChannelsHiddenLayerSimilarityPredictor(), config.getNumChannelsOutputLayerSimilarityPredictor()));
        } else {
            similarityProjectorBlock = null;
            similarityPredictorBlock = null;
        }

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
        NDList stateForPrediction = firstHalfNDList(representationResult);
        NDList stateForTimeEvolution = secondHalfNDList(representationResult);

        NDList predictionResult = predictionBlock.forward(parameterStore, stateForPrediction, training, params);
        for (NDArray prediction : predictionResult.getResourceNDArrays()) {
            combinedResult.add(prediction);
        }

        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {


            // recurrent Inference

            predictionBlock.setWithReward(true);

            NDArray action = inputs.get(config.isWithConsistencyLoss() ? 2 * k - 1 : k );

            NDList dynamicIn = new NDList();
            dynamicIn.addAll(stateForTimeEvolution);
            dynamicIn.add(action);

            NDList dynamicsResult = dynamicsBlock.forward(parameterStore, dynamicIn, training, params);

             stateForPrediction = firstHalfNDList(dynamicsResult);
             stateForTimeEvolution = secondHalfNDList(dynamicsResult);


            predictionResult = predictionBlock.forward(parameterStore, stateForPrediction, training, params);

            if (config.isWithConsistencyLoss()) {

                NDList similarityProjectorResultList = this.similarityProjectorBlock.forward(parameterStore, new NDList(stateForTimeEvolution.get(1)), training, params);
                NDArray similarityPredictorResult = this.similarityPredictorBlock.forward(parameterStore, similarityProjectorResultList, training, params).get(1);


                representationResult = representationBlock.forward(parameterStore, new NDList(inputs.get(2 * k)), training, params);
                NDArray similarityProjectorResultLabel = this.similarityProjectorBlock.forward(parameterStore, new NDList(representationResult.get(stateForPrediction.size())), training, params).get(1);
                similarityProjectorResultLabel = similarityProjectorResultLabel.stopGradient();

                combinedResult.add(similarityPredictorResult);
                combinedResult.add(similarityProjectorResultLabel);
            }

            for (NDArray prediction : predictionResult.getResourceNDArrays()) {
                combinedResult.add(prediction);
            }

            NDList temp = new NDList();
            for (int i = 0; i < stateForTimeEvolution.size(); i++) {
                temp.add(stateForTimeEvolution.get(i).scaleGradient(0.5));
            }
            stateForTimeEvolution = temp;

        }
        return combinedResult;
    }

public static Shape[] firstHalf(Shape[] inputShapes) {
        int half = inputShapes.length / 2;
        Shape[] outputShapes = new Shape[half];
        for (int i = 0; i < half; i++) {
            outputShapes[i] = inputShapes[i];
        }
        return outputShapes;
    }
    public static Shape[] secondHalf(Shape[] inputShapes) {
        int half = inputShapes.length / 2;
        Shape[] outputShapes = new Shape[half];
        for (int i = 0; i < half; i++) {
            outputShapes[i] = inputShapes[half + i];
        }
        return outputShapes;
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] outputShapes = new Shape[0];

        // initial Inference
        predictionBlock.setWithReward(false);
        Shape[] stateOutputShapes = representationBlock.getOutputShapes(new Shape[]{inputShapes[0]});

        Shape[] stateOutputShapesForPrediction = firstHalf(stateOutputShapes);
        Shape[] stateOutputShapesForTimeEvolution = secondHalf(stateOutputShapes);

        Shape[] predictionBlockOutputShapes = predictionBlock.getOutputShapes(stateOutputShapesForPrediction);
        outputShapes = ArrayUtils.addAll(stateOutputShapesForTimeEvolution, predictionBlockOutputShapes);

        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {
            // recurrent Inference
            predictionBlock.setWithReward(true);
            Shape stateShape = stateOutputShapesForTimeEvolution[0];
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

            stateOutputShapesForPrediction = firstHalf(stateOutputShapes);
             stateOutputShapesForTimeEvolution = secondHalf(stateOutputShapes);



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

        if (config.isWithConsistencyLoss()) {
            Shape similarityProjectorInputShape = secondHalf(stateOutputShapes)[0];
            similarityProjectorBlock.initialize(manager, dataType, similarityProjectorInputShape);

            Shape[] projectorOutputShapes = similarityProjectorBlock.getOutputShapes(new Shape[]{similarityProjectorInputShape});
            this.similarityPredictorBlock.initialize(manager, dataType, projectorOutputShapes[0]);
        }

        Shape[] predictionInputShape =  firstHalf(stateOutputShapes);



        predictionBlock.setWithReward(true);
        predictionBlock.initialize(manager, dataType, predictionInputShape);


        Shape actionShape = inputShapes[1];
        Shape[] dynamicsInputShape = getDynamicsInputShape(stateOutputShapes, actionShape);


        dynamicsBlock.initialize(manager, dataType, dynamicsInputShape);


    }

    @NotNull
    private Shape[] getDynamicsInputShape(Shape[] stateOutputShapes, Shape actionShape) {
        Shape[] dynamicsInputShape;
        Shape[] dynamicsInputShapeWithoutAction = secondHalf(stateOutputShapes);
        dynamicsInputShape = new Shape[dynamicsInputShapeWithoutAction.length + 1];
        System.arraycopy(dynamicsInputShapeWithoutAction, 0, dynamicsInputShape, 0, dynamicsInputShapeWithoutAction.length);
        dynamicsInputShape[dynamicsInputShapeWithoutAction.length] = actionShape;
        return dynamicsInputShape;
    }


    @Override
    public void freeze(boolean[] freeze) {
        this.predictionBlock.freeze(freeze);
        this.dynamicsBlock.freeze(freeze);
        if (config.isWithConsistencyLoss()) {
            this.similarityPredictorBlock.freezeParameters(freeze[0]);
            this.similarityProjectorBlock.freezeParameters(freeze[0]);
        }
        this.representationBlock.freeze(freeze);
    }
}
