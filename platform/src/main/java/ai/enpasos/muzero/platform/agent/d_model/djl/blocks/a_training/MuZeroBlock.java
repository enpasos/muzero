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
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.*;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.InitialInferenceBlock.firstHalfNDList;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.InitialInferenceBlock.secondHalfNDList;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.DynamicsBlock.newDynamicsBlock;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.RepresentationBlock.newRepresentationBlock;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class MuZeroBlock extends AbstractBlock implements DCLAware {

    private final RepresentationBlock representationBlock;
    private final PredictionBlock predictionBlock;
    private final DynamicsBlock dynamicsBlock;

    private final MuZeroConfig config;
    private final SimilarityPredictorBlock similarityPredictorBlock;
    private final SimilarityProjectorBlock similarityProjectorBlock;


    private boolean rulesModel = false;

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

            int numUnrollSteps = config.getNumUnrollSteps();
            if (isRulesModel()) {
                numUnrollSteps = this.numUnrollSteps;
            }

            // added predictions to the combinedResult in the following order:
            // initial inference
            // - rules layer: legal actions
            // - policy layer:  policy
            // - value layer:  value
            // each recurrent inference
            // - rules layer: consistency loss
            // - rules layer: legal actions
            // - rules layer: reward
            // - policy layer:  policy
            // - value layer:  value

            // initial Inference
            predictionBlock.setWithReward(false);

            predictionBlock.setWithValue(isRulesModel() ? false : true);
            predictionBlock.setWithPolicy(isRulesModel() ? false : true);
            predictionBlock.setWithLegalAction(true);

            NDList representationResult = representationBlock.forward(parameterStore, new NDList(inputs.get(0)), training, params);
            NDList stateForPrediction = firstHalfNDList(representationResult);
            NDList stateForTimeEvolution = secondHalfNDList(representationResult);

            NDList predictionResult = predictionBlock.forward(parameterStore, stateForPrediction, training, params);
            for (NDArray prediction : predictionResult.getResourceNDArrays()) {
                combinedResult.add(prediction);
            }

            for (int k = 1; k <= numUnrollSteps; k++) {
                // recurrent Inference
                predictionBlock.setWithReward(true);

                NDArray action = inputs.get(config.isWithConsistencyLoss() ? 2 * k - 1 : k);

                NDList dynamicIn = new NDList();
                dynamicIn.addAll(stateForTimeEvolution);
                dynamicIn.add(action);

                NDList dynamicsResult = dynamicsBlock.forward(parameterStore, dynamicIn, training, params);

                stateForPrediction = firstHalfNDList(dynamicsResult);
                stateForTimeEvolution = secondHalfNDList(dynamicsResult);

//                if (k == numUnrollSteps) {
//                    predictionBlock.setWithLegalAction(false);
//                }
                predictionResult = predictionBlock.forward(parameterStore, stateForPrediction, training, params);

                if (config.isWithConsistencyLoss()) {

                    NDList similarityProjectorResultList = this.similarityProjectorBlock.forward(parameterStore, new NDList(stateForTimeEvolution.get(0)), training, params);
                    NDArray similarityPredictorResult = this.similarityPredictorBlock.forward(parameterStore, similarityProjectorResultList, training, params).get(0);

                    representationResult = representationBlock.forward(parameterStore, new NDList(inputs.get(2 * k)), training, params);
                    NDArray similarityProjectorResultLabel = this.similarityProjectorBlock.forward(parameterStore, new NDList(representationResult.get(3)), training, params).get(0);
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

    private int numUnrollSteps = 0;
    public void setNumUnrollSteps(int numUnrollSteps) {
        this.numUnrollSteps = numUnrollSteps;
        if (rulesModel) {
            setRulesModelInputNames();
        }
    }
    private void setRulesModelInputNames() {
        inputNames = new ArrayList<>();
        inputNames.add("observation");
        for (int k = 0; k < this.getNumUnrollSteps() ; k++) {
            inputNames.add("action_" + k);
        }
    }



    public int getNumUnrollSteps() {
        return this.numUnrollSteps;
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


        int numUnrollSteps = config.getNumUnrollSteps();
        if (isRulesModel()) {
            numUnrollSteps = this.numUnrollSteps;
        }

        Shape[] outputShapes = new Shape[0];

        // initial Inference
        predictionBlock.setWithReward(false);
        Shape[] stateOutputShapes = representationBlock.getOutputShapes(new Shape[]{inputShapes[0]});

        Shape[] stateOutputShapesForPrediction = firstHalf(stateOutputShapes);
        Shape[] stateOutputShapesForTimeEvolution = secondHalf(stateOutputShapes);

        Shape[] predictionBlockOutputShapes = predictionBlock.getOutputShapes(stateOutputShapesForPrediction);
        outputShapes = ArrayUtils.addAll(stateOutputShapesForTimeEvolution, predictionBlockOutputShapes);

        for (int k = 1; k <= numUnrollSteps; k++) {
            // recurrent Inference
            predictionBlock.setWithReward(true);
            Shape stateShape = stateOutputShapes[0];
            Shape actionShape = inputShapes[k];
            Shape[] dynamicInShape = ArrayUtils.addAll(stateOutputShapesForTimeEvolution, actionShape);

            stateOutputShapes = dynamicsBlock.getOutputShapes( dynamicInShape );


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
        if (similarityProjectorBlock != null && this.similarityPredictorBlock != null) {
            similarityProjectorBlock.initialize(manager, dataType, stateOutputShapes[3]);
            Shape[] projectorOutputShapes = similarityProjectorBlock.getOutputShapes(new Shape[]{stateOutputShapes[3]});

                this.similarityPredictorBlock.initialize(manager, dataType, projectorOutputShapes[0]);

        }

        Shape[] predictionInputShape = new Shape[3];
        predictionInputShape[0] = stateOutputShapes[0];
        predictionInputShape[1] = stateOutputShapes[1];
        predictionInputShape[2] = stateOutputShapes[2];


        predictionBlock.setWithReward(true);
        predictionBlock.setWithValue(true);
        predictionBlock.setWithPolicy(true);
        predictionBlock.setWithLegalAction(true);
        predictionBlock.initialize(manager, dataType, predictionInputShape);

        if(inputShapes.length > 1) {
            Shape actionShape = inputShapes[1];
            Shape[] dynamicsInputShape = getDynamicsInputShape(stateOutputShapes, actionShape);
            dynamicsBlock.initialize(manager, dataType, dynamicsInputShape);
        }

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


//    public Shape[] getOutputShapesRulesModel(Shape[] inputShapes) {
//        Shape[] outputShapes = new Shape[0];
//
//        // initial Inference
//        predictionBlock.setWithReward(false);
//        predictionBlock.setWithValue(false);
//        predictionBlock.setWithLegalAction(true);
//        predictionBlock.setWithPolicy(false);
//        Shape[] stateOutputShapes = representationBlock.getOutputShapes(new Shape[]{inputShapes[0]});
//
//        Shape[] stateOutputShapesForPrediction = firstHalf(stateOutputShapes);
//        Shape[] stateOutputShapesForTimeEvolution = secondHalf(stateOutputShapes);
//
//        Shape[] predictionBlockOutputShapes = predictionBlock.getOutputShapes(stateOutputShapesForPrediction);
//        outputShapes = ArrayUtils.addAll(stateOutputShapesForTimeEvolution, predictionBlockOutputShapes);
//
//        int k = 1;
//        // recurrent Inference
//
//        predictionBlock.setWithReward(true);
//
//        Shape stateShape = stateOutputShapes[0];
//        Shape actionShape = inputShapes[k];
//        Shape[] dynamicInShape = ArrayUtils.addAll(stateOutputShapesForTimeEvolution, actionShape);
//
//        stateOutputShapes = dynamicsBlock.getOutputShapes( dynamicInShape );
//
//        stateOutputShapesForPrediction = firstHalf(stateOutputShapes);
//
//        outputShapes = ArrayUtils.addAll(outputShapes, predictionBlock.getOutputShapes(stateOutputShapesForPrediction));
//
//        return outputShapes;
//    }





    @Override
    public void freezeParameters(boolean[] freeze) {
        this.predictionBlock.freezeParameters(freeze);
        this.dynamicsBlock.freezeParameters(freeze);
        if (this.similarityPredictorBlock != null && this.similarityProjectorBlock != null) {
            this.similarityPredictorBlock.freezeParameters(freeze[0]);
            this.similarityProjectorBlock.freezeParameters(freeze[0]);
        }
        this.representationBlock.freezeParameters(freeze);
    }

    @Override
    public void setExportFilter(boolean[] exportFilter) {
        this.predictionBlock.setExportFilter(exportFilter);
        this.dynamicsBlock.setExportFilter(exportFilter);
        if (this.similarityPredictorBlock != null && this.similarityProjectorBlock != null) {
            this.similarityPredictorBlock.setExportFilter(exportFilter);
            this.similarityProjectorBlock.setExportFilter(exportFilter);
        }

        this.representationBlock.setExportFilter(exportFilter);
    }

    public boolean isRulesModel() {
        return rulesModel;
    }

    public void setRulesModel(boolean rulesModel) {
        this.rulesModel = rulesModel;
//        if (rulesModel) {
//            setRulesModelInputNames();
//        } else {
//            inputNames = new ArrayList<>();
//            inputNames.add("observation");
//            for (int k = 1; k <= config.getNumUnrollSteps(); k++) {
//                inputNames.add("action_" + k);
//            }
//        }
    }
}
