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
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.DynamicsBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.PredictionBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.PredictionHeads;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.common.MuZeroException;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.InitialInferenceBlock.*;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class RecurrentInferenceBlock extends AbstractBlock implements OnnxIO, DCLAware {

    private final DynamicsBlock g;
    private final PredictionBlock f;

    public RecurrentInferenceBlock(DynamicsBlock dynamicsBlock, PredictionBlock predictionBlock) {
        super(MYVERSION);
        g = this.addChildBlock("Dynamics", dynamicsBlock);
        f = this.addChildBlock("Prediction", predictionBlock);
    }

    public DynamicsBlock getG() {
        return g;
    }

    public PredictionBlock getF() {
        return f;
    }

    /**
     * @param inputs First input for state, second for action
     */
    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, @NotNull NDList inputs, boolean training, PairList<String, Object> params) {
        NDList gResult = g.forward(parameterStore, inputs, training);
        f.getPredictionHeads().setWithReward(true);
        f.getPredictionHeads().setWithValue(true);
        f.getPredictionHeads().setWithPolicy(true);
        f.getPredictionHeads().setWithLegalAction(true);
        NDList predictionInput = new NDList(gResult.get(0), gResult.get(1), gResult.get(2), inputs.get(3));
        NDList fResult = f.forward(parameterStore,  predictionInput, training, params);
        NDList result =  gResult;
        return result.addAll(fResult);
    }


    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        f.getPredictionHeads().setWithReward(true);
        Shape[] gOutputShapes = g.getOutputShapes(inputShapes);

        Shape[] gOutputShapesForPrediction =  gOutputShapes ;
        Shape[] gOutputShapesForTimeEvolution = gOutputShapes ;


        Shape[] fOutputShapes = f.getOutputShapes(gOutputShapesForPrediction);
        return ArrayUtils.addAll(gOutputShapesForTimeEvolution, fOutputShapes);
    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
throw new MuZeroException("implemented in MuZeroBlock");
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

        List<OnnxTensor> gOutputForF =   new ArrayList<>(gOutput);
        List<OnnxTensor> gOutputForG =   new ArrayList<>(gOutput);

        gOutputForF.add(input.get(1));

        OnnxBlock fOnnx = f.getOnnxBlock(counter, gOutputForF);
        onnxBlock.addChild(fOnnx);
        List<OnnxTensor> fOutput = fOnnx.getOutput();

        onnxBlock.getValueInfos().addAll(createValueInfoProto(input));

        List<OnnxTensor> totalOutput = new ArrayList<>();
        totalOutput.addAll(gOutputForG);
        totalOutput.addAll(fOutput);

        onnxBlock.setOutput(totalOutput);

        return onnxBlock;
    }

//    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
//
//        OnnxBlock onnxBlock = OnnxBlock.builder()
//                .input(input)
//                .build();
//
//
//        OnnxBlock gOnnx = h.getOnnxBlock(counter, List.of(input.get(0)));
//        onnxBlock.addChild(gOnnx);
//        List<OnnxTensor> gOutput = gOnnx.getOutput();
//
//        List<OnnxTensor> gOutputForF =   new ArrayList<>(gOutput);
//        List<OnnxTensor> gOutputForG =   new ArrayList<>(gOutput);
//
//        gOutputForF.add(input.get(1));
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

    @Override
    public void freezeParameters(boolean[] freeze) {
        this.f.freezeParameters(freeze);
        this.g.freezeParameters(freeze);
    }

    @Override
    public void setExportFilter(boolean[] exportFilter) {
        this.f.setExportFilter(exportFilter);
        this.g.setExportFilter(exportFilter);
    }
}
