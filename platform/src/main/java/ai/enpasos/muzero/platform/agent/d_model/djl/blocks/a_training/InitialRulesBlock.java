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
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.CausalityFreezing;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.DynamicsBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.PredictionBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.RepresentationBlock;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock.firstHalf;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock.secondHalf;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.InitialInferenceBlock.*;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class InitialRulesBlock extends AbstractBlock { //implements OnnxIO  {

    private final RepresentationBlock representationBlock;
    private final PredictionBlock predictionBlock;

    private final DynamicsBlock dynamicsBlock;

    public InitialRulesBlock(RepresentationBlock representationBlock, PredictionBlock predictionBlock, DynamicsBlock dynamicsBlock, MuZeroConfig config) {
        super(MYVERSION);

        this.representationBlock = this.addChildBlock("Representation", representationBlock.getBlockForInitialRulesOnly(config));
        this.predictionBlock = this.addChildBlock("Prediction", predictionBlock);
        this.dynamicsBlock = this.addChildBlock("Dynamics", dynamicsBlock);
    }

//    public RepresentationBlock getH() {
//        return representationBlock;
//    }
//
//    public PredictionBlock getF() {
//        return predictionBlock;
//    }

    @Override
    protected NDList forwardInternal(@NotNull ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {

        NDList combinedResult = new NDList();

        // just the lines with * are relevant here
        // added predictions to the combinedResult in the following order:
        // initial inference
        // - rules layer: legal actions  *
        // - policy layer:  policy
        // - value layer:  value
        // each recurrent inference
        // - rules layer: consistency loss
        // - rules layer: reward  *
        // - rules layer: legal actions
        // - policy layer:  policy
        // - value layer:  value

        // initial Inference
        predictionBlock.setHeadUsage(new boolean[]{true, false, false, false});
        NDList representationResult = representationBlock.forward(parameterStore, new NDList(inputs.get(0)), training, params);
        NDList stateForPrediction = firstHalfNDList(representationResult);
        NDList stateForTimeEvolution = secondHalfNDList(representationResult);

        NDList predictionResult = predictionBlock.forward(parameterStore, stateForPrediction, training, params);
        for (NDArray prediction : predictionResult.getResourceNDArrays()) {
            combinedResult.add(prediction);
        }

        int k = 1;


        // recurrent Inference

        predictionBlock.setHeadUsage(new boolean[]{false, true, false, false});

        NDArray action = inputs.get(k);

        NDList dynamicIn = new NDList();
        dynamicIn.addAll(stateForTimeEvolution);
        dynamicIn.add(action);

        NDList dynamicsResult = dynamicsBlock.forward(parameterStore, dynamicIn, training, params);

        stateForPrediction = firstHalfNDList(dynamicsResult);
        stateForTimeEvolution = secondHalfNDList(dynamicsResult);


        predictionResult = predictionBlock.forward(parameterStore, stateForPrediction, training, params);


        for (NDArray prediction : predictionResult.getResourceNDArrays()) {
            combinedResult.add(prediction);
        }


        return combinedResult;
    }


    /** {@inheritDoc} */
    @Override
    public boolean isInitialized() {
        return representationBlock.isInitialized() && predictionBlock.isInitialized() && dynamicsBlock.isInitialized();

    }

    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        this.representationBlock.initialize(manager, dataType, inputShapes );









        // initial Inference
        predictionBlock.setHeadUsage(new boolean[]{true, false, true, true});
        Shape[] stateOutputShapes = representationBlock.getOutputShapes(new Shape[]{inputShapes[0]});

        Shape[] stateOutputShapesForPrediction = firstHalf(stateOutputShapes);
        Shape[] stateOutputShapesForTimeEvolution = secondHalf(stateOutputShapes);

        this.predictionBlock.initialize(manager, dataType, stateOutputShapesForPrediction);
        Shape[] predictionBlockOutputShapes = predictionBlock.getOutputShapes(stateOutputShapesForPrediction);


        int k = 1;
        // recurrent Inference
        predictionBlock.setHeadUsage(new boolean[]{true, true, true, true});

        Shape actionShape = inputShapes[k];
        Shape[] dynamicInShape = ArrayUtils.addAll(stateOutputShapesForTimeEvolution, actionShape);

        this.dynamicsBlock.initialize(manager, dataType, dynamicInShape);
        stateOutputShapes = dynamicsBlock.getOutputShapes(dynamicInShape);

        stateOutputShapesForPrediction = firstHalf(stateOutputShapes);
        //  stateOutputShapesForTimeEvolution = secondHalf(stateOutputShapes);

        this.predictionBlock.initialize(manager, dataType, stateOutputShapesForPrediction);
    }



    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] outputShapes = new Shape[0];

        // initial Inference
        predictionBlock.setHeadUsage(new boolean[]{true, false, true, true});
        Shape[] stateOutputShapes = representationBlock.getOutputShapes(new Shape[]{inputShapes[0]});

        Shape[] stateOutputShapesForPrediction = firstHalf(stateOutputShapes);
        Shape[] stateOutputShapesForTimeEvolution = secondHalf(stateOutputShapes);

        Shape[] predictionBlockOutputShapes = predictionBlock.getOutputShapes(stateOutputShapesForPrediction);
        outputShapes = ArrayUtils.addAll(stateOutputShapesForTimeEvolution, predictionBlockOutputShapes);

        int k = 1;
        // recurrent Inference
        predictionBlock.setHeadUsage(new boolean[]{true, true, true, true});

        Shape actionShape = inputShapes[k];
        Shape[] dynamicInShape = ArrayUtils.addAll(stateOutputShapesForTimeEvolution, actionShape);

        stateOutputShapes = dynamicsBlock.getOutputShapes(dynamicInShape);

        stateOutputShapesForPrediction = firstHalf(stateOutputShapes);
        //  stateOutputShapesForTimeEvolution = secondHalf(stateOutputShapes);

        outputShapes = ArrayUtils.addAll(outputShapes, predictionBlock.getOutputShapes(stateOutputShapesForPrediction));

        return outputShapes;
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
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("\nInitialInference(\n");
        for (Block block : children.values()) {
            String blockString = block.toString().replaceAll("(?m)^", "\t");
            sb.append(blockString).append('\n');
        }
        sb.append(')');
        return sb.toString().replace("\t", "  ");
    }

//    public static NDList firstHalfNDList(NDList list) {
//        return list.subNDList(0, list.size()/2);
//    }
//    public static NDList secondHalfNDList(NDList list) {
//        return list.subNDList(list.size()/2, list.size());
//    }
//
//    public static List<OnnxTensor> firstHalfList(List<OnnxTensor> list) {
//        return list.subList(0, list.size()/2);
//    }
//    public static List<OnnxTensor> secondHalfList(List<OnnxTensor> list) {
//        return list.subList(list.size()/2, list.size());
//    }


//    @Override
//    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
//
//        OnnxBlock onnxBlock = OnnxBlock.builder()
//            .input(input)
//            .build();
//
//
//        OnnxBlock gOnnx = h.getOnnxBlock(counter, List.of(input.get(0)));
//        onnxBlock.addChild(gOnnx);
//        List<OnnxTensor> gOutput = gOnnx.getOutput();
//
//        List<OnnxTensor> gOutputForF =  firstHalfList(gOutput);
//        List<OnnxTensor> gOutputForG =  secondHalfList(gOutput);
//
//
//
//        OnnxBlock fOnnx = f.getOnnxBlock(counter, gOutputForF);
//        onnxBlock.addChild(fOnnx);
//        List<OnnxTensor> fOutput = fOnnx.getOutput();
//
//        onnxBlock.getValueInfos().addAll(createValueInfoProto(input));
//
//        List<OnnxTensor> totalOutput = new ArrayList<>();
//        totalOutput.addAll(gOutputForG);
//        totalOutput.addAll(fOutput);
//
//        onnxBlock.setOutput(totalOutput);
//
//        return onnxBlock;
//    }


}
