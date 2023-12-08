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

package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference;

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
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.*;
import ai.enpasos.muzero.platform.common.MuZeroException;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class RecurrentInferenceBlock extends AbstractBlock implements OnnxIO {

    private final DynamicsBlock g;
    private final Representation2Block h2;

    private final Representation3Block h3;
    private final PredictionBlock f;
    private final LegalActionsBlock f1;
    private final RewardBlock f2;

    public RecurrentInferenceBlock(DynamicsBlock dynamicsBlock,  Representation2Block representation2Block, Representation3Block representation3Block, PredictionBlock predictionBlock, LegalActionsBlock legalActionsBlock, RewardBlock rewardBlock) {
        super(MYVERSION);
        g = this.addChildBlock("Dynamics", dynamicsBlock);
        h2 = this.addChildBlock("Representation2", representation2Block);
        h3 = this.addChildBlock("Representation3", representation3Block);
        f = this.addChildBlock("Prediction", predictionBlock);
        f1 = this.addChildBlock("LegalActions", legalActionsBlock);
        f2 = this.addChildBlock("Reward", rewardBlock);

    }


    public Representation2Block getH2() {
        return h2;
    }
    public DynamicsBlock getG() {
        return g;
    }
        public RewardBlock getf2() {
        return f2;
    }
    public LegalActionsBlock getf1() {
        return f1;
    }

    public PredictionBlock getF() {
        return f;
    }

    /**
     * @param inputs First input for state, second for action
     */
    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, @NotNull NDList inputs, boolean training, PairList<String, Object> params) {
        NDList gResult = g.forward(parameterStore, inputs, training, params);

        NDArray rulesState = gResult.get(0);
        NDArray action = inputs.get(1);

        // rules layer - reward
        NDList rewardIn = new NDList(rulesState, action);  // here it is the rulesState after dynamicsBlock

        NDList h3Result = h3.forward(parameterStore, new NDList(rewardIn), training, params);


        NDList h2Result = h2.forward(parameterStore, gResult, training, params);
        NDList f1Result = f1.forward(parameterStore, h3Result, training, params);
        NDList f2Result = f2.forward(parameterStore, h3Result, training, params);
        NDList fResult = f.forward(parameterStore, h2Result, training, params);
        return gResult.addAll(f1Result).addAll(f2Result).addAll(fResult);

    }


    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape state1Shape = inputShapes[0];
        Shape actionShape = inputShapes[1];
        long[] shapeArray = state1Shape.getShape();
        shapeArray[1] += actionShape.get(1);
        Shape dynamicsInputShape = new Shape(shapeArray);
        Shape[] state3Shapes = g.getOutputShapes(new Shape[]{dynamicsInputShape});


        shapeArray = state3Shapes[0].getShape();
        shapeArray[1] += actionShape.get(1);
        Shape rewardInputShape = new Shape(shapeArray);

        return (Shape[]) ArrayUtils.addAll(state3Shapes,
                f1.getOutputShapes(state3Shapes), f2.getOutputShapes(new Shape[]{rewardInputShape}));

    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        throw new MuZeroException("not implemented - implemented in MuZeroBlock");
//        g.initialize(manager, dataType, inputShapes[0]);
//
//
//        Shape[] state1OutputShapes = g.getOutputShapes(new Shape[]{inputShapes[0]});
//        f1.initialize(manager, dataType, state1OutputShapes[0]);
//
//
//        h2.initialize(manager, dataType,  state1OutputShapes[0]);
//        Shape[] state2OutputShapes = h2.getOutputShapes(state1OutputShapes);
//        f.initialize(manager, dataType, state2OutputShapes[0]);
//
//
////        similarityProjectorBlock.initialize(manager, dataType, state1OutputShapes[0]);
////        Shape[] projectorOutputShapes = similarityProjectorBlock.getOutputShapes(new Shape[]{state1OutputShapes[0]});
////        similarityPredictorBlock.initialize(manager, dataType, projectorOutputShapes[0]);
//
//        Shape state1Shape = state1OutputShapes[0];
//        Shape actionShape = inputShapes[1];
//        g.initialize(manager, dataType, state1Shape, actionShape);
//
//          f2.initialize(manager, dataType, state1Shape, actionShape);
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


    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {

        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .build();

        OnnxBlock gOnnx = g.getOnnxBlock(counter, input);
        onnxBlock.addChild(gOnnx);
        List<OnnxTensor> gOutput = gOnnx.getOutput();

        OnnxBlock f1Onnx = f1.getOnnxBlock(counter, gOutput);
        onnxBlock.addChild(f1Onnx);
        List<OnnxTensor> f1Output = f1Onnx.getOutput();



        OnnxBlock f2Onnx = f2.getOnnxBlock(counter, gOutput);
        onnxBlock.addChild(f2Onnx);
        List<OnnxTensor> f2Output = f2Onnx.getOutput();



        OnnxBlock h2Onnx = h2.getOnnxBlock(counter, gOnnx.getOutput());
        onnxBlock.addChild(h2Onnx);


        List<OnnxTensor> h2Output = h2Onnx.getOutput();
        OnnxBlock fOnnx = f.getOnnxBlock(counter, h2Output);
        onnxBlock.addChild(fOnnx);

        List<OnnxTensor> fOutput = fOnnx.getOutput();



        onnxBlock.getValueInfos().addAll(createValueInfoProto(input));

        List<OnnxTensor> totalOutput = new ArrayList<>();
        totalOutput.addAll(gOutput);
        totalOutput.addAll(f1Output);
        totalOutput.addAll(f2Output);
        totalOutput.addAll(fOutput);

        onnxBlock.setOutput(totalOutput);

        return onnxBlock;
    }
}
