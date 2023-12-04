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
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.*;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.DynamicsBlock.newDynamicsBlock;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.Representation1Block.newRepresentation1Block;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.Representation2Block.newRepresentation2Block;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class MuZeroBlock extends AbstractBlock {

    private final Representation1Block representation1Block;

    private final RewardBlock rewardBlock;
    private final Representation2Block representation2Block;
    private final LegalActionsBlock legalActionsBlock;
    private final PredictionBlock predictionBlock;
    private final DynamicsBlock dynamicsBlock;

    private final MuZeroConfig config;
    private final SimilarityPredictorBlock similarityPredictorBlock;
    private final SimilarityProjectorBlock similarityProjectorBlock;

    public MuZeroBlock(MuZeroConfig config) {
        super(MYVERSION);
        this.config = config;


        representation1Block = this.addChildBlock("Representation1", newRepresentation1Block(config));
        dynamicsBlock = this.addChildBlock("Dynamics", newDynamicsBlock(config));
        rewardBlock = this.addChildBlock("Reward", new RewardBlock(config));
        legalActionsBlock = this.addChildBlock("LegalActions", new LegalActionsBlock(config));

        representation2Block = this.addChildBlock("Representation2", newRepresentation2Block(config));
        predictionBlock = this.addChildBlock("Prediction", new PredictionBlock(config));

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
        // - policy layer:  value
        // each recurrent inference
        // - rules layer: legal actions
        // - rules layer: reward
        // - rules layer: consistency loss
        // - policy layer:  policy
        // - policy layer:  value


        // <<< initial Inference

        // rules layer
        NDList representation1Result = representation1Block.forward(parameterStore, new NDList(inputs.get(0)), training, params);
        NDArray rulesState = representation1Result.get(0);

        NDList predictionLegalActionsResult = legalActionsBlock.forward(parameterStore, representation1Result, training, params);
        combinedResult.add(predictionLegalActionsResult.get(0));


        // policy layer
        // the rules is not trained from the policy layer!
        // as the rules do not depend on the policy - only in the other direction
        NDList inputToPolicyLayer = new NDList(rulesState.stopGradient());
        NDList representation2Result = representation2Block.forward(parameterStore, inputToPolicyLayer, training, params);
        NDList predictionResult = predictionBlock.forward(parameterStore, representation2Result, training, params);
        for (NDArray prediction : predictionResult.getResourceNDArrays()) {
            combinedResult.add(prediction);
        }

        // initial Inference >>>


        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {


            // <<< recurrent Inference k

            // rules layer
            NDArray action = inputs.get(2 * k - 1);
            NDList dynamicIn = new NDList(rulesState, action);

            NDList dynamicsResult = dynamicsBlock.forward(parameterStore, dynamicIn, training, params);
            rulesState = dynamicsResult.get(0);

            // rules layer - legal actions
            predictionLegalActionsResult = legalActionsBlock.forward(parameterStore, representation1Result, training, params);
            combinedResult.add(predictionLegalActionsResult.get(0));


            // rules layer - reward
            NDList rewardIn = new NDList(rulesState, action);  // here it is the rulesState after dynamicsBlock
            NDList rewardOut = rewardBlock.forward(parameterStore, rewardIn, training, params);
            combinedResult.add(rewardOut.get(0));


            // rules layer - consistency loss
            NDList similarityProjectorResultList = this.similarityProjectorBlock.forward(parameterStore, new NDList(rulesState), training, params);
            NDArray similarityPredictorResult = this.similarityPredictorBlock.forward(parameterStore, similarityProjectorResultList, training, params).get(0);
            NDArray similarityProjectorResultLabel = this.similarityProjectorBlock.forward(parameterStore, representation1Result, training, params).get(0);
            similarityProjectorResultLabel = similarityProjectorResultLabel.stopGradient();
            combinedResult.add(similarityPredictorResult);
            combinedResult.add(similarityProjectorResultLabel);


            // policy layer
            inputToPolicyLayer = new NDList(rulesState.stopGradient());
            representation2Result = representation2Block.forward(parameterStore, inputToPolicyLayer, training, params);
            predictionResult = predictionBlock.forward(parameterStore, representation2Result, training, params);
            for (NDArray prediction : predictionResult.getResourceNDArrays()) {
                combinedResult.add(prediction);
            }


            // recurrent Inference k >>>

            rulesState = rulesState.scaleGradient(0.5);

        }
        return combinedResult;
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] outputShapes = new Shape[0];
        // added predictions shapes to outputShapes in the following order:
        // initial inference
        // - rules layer: legal actions
        // - policy layer:  policy
        // - policy layer:  value
        // each recurrent inference
        // - rules layer: legal actions
        // - rules layer: reward
        // - rules layer: consistency loss
        // - policy layer:  policy
        // - policy layer:  value

        // initial Inference
        Shape[] state1OutputShapes = representation1Block.getOutputShapes(new Shape[]{inputShapes[0]});
        outputShapes = ArrayUtils.addAll(outputShapes, legalActionsBlock.getOutputShapes(state1OutputShapes));
        Shape[] state2OutputShapes = representation2Block.getOutputShapes(state1OutputShapes);
        outputShapes = ArrayUtils.addAll(outputShapes, predictionBlock.getOutputShapes(state2OutputShapes));


        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {
            // recurrent Inference
            outputShapes = ArrayUtils.addAll(outputShapes, legalActionsBlock.getOutputShapes(state1OutputShapes));
            Shape state1Shape = state1OutputShapes[0];

            Shape actionShape = inputShapes[k];
            long[] shapeArray = state1Shape.getShape();
            shapeArray[1] += actionShape.get(1);
            Shape dynamicsInputShape = new Shape(shapeArray);
            Shape[] state3Shapes = dynamicsBlock.getOutputShapes(new Shape[]{dynamicsInputShape});


            outputShapes = ArrayUtils.addAll(outputShapes, legalActionsBlock.getOutputShapes(state3Shapes));


            shapeArray = state3Shapes[0].getShape();
            shapeArray[1] += actionShape.get(1);
            Shape rewardInputShape = new Shape(shapeArray);
            outputShapes = ArrayUtils.addAll(outputShapes, rewardBlock.getOutputShapes(new Shape[]{rewardInputShape}));


            // rules layer - consistency loss - no output shape


            state2OutputShapes = representation2Block.getOutputShapes(state1OutputShapes);
            outputShapes = ArrayUtils.addAll(outputShapes, predictionBlock.getOutputShapes(state2OutputShapes));

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
        representation1Block.initialize(manager, dataType, inputShapes[0]);


        Shape[] state1OutputShapes = representation1Block.getOutputShapes(new Shape[]{inputShapes[0]});
        legalActionsBlock.initialize(manager, dataType, state1OutputShapes[0]);


        representation2Block.initialize(manager, dataType,  state1OutputShapes[0]);
        Shape[] state2OutputShapes = representation2Block.getOutputShapes(state1OutputShapes);
        predictionBlock.initialize(manager, dataType, state2OutputShapes[0]);


        similarityProjectorBlock.initialize(manager, dataType, state1OutputShapes[0]);
        Shape[] projectorOutputShapes = similarityProjectorBlock.getOutputShapes(new Shape[]{state1OutputShapes[0]});
        similarityPredictorBlock.initialize(manager, dataType, projectorOutputShapes[0]);

        Shape state1Shape = state1OutputShapes[0];
        Shape actionShape = inputShapes[1];
        dynamicsBlock.initialize(manager, dataType, state1Shape, actionShape);

        rewardBlock.initialize(manager, dataType, state1Shape, actionShape);
    }


}
