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
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.CausalityFreezing;
import ai.enpasos.muzero.platform.common.MuZeroException;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class RecurrentInferenceBlock extends AbstractBlock implements OnnxIO, CausalityFreezing {

    private final DynamicsBlock g;
    private final PredictionBlock f;

    public RecurrentInferenceBlock(DynamicsBlock dynamicsBlock, PredictionBlock predictionBlock ) {
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
        f.setWithReward(true);
        NDList fResult = f.forward(parameterStore, new NDList(gResult.get(0), gResult.get(1),gResult.get(2)), training, params);
        NDList result = new NDList(gResult.get(3), gResult.get(4),gResult.get(5));
        return result.addAll(fResult);
    }


    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        f.setWithReward(true);
        Shape[] gOutputShapes = g.getOutputShapes(inputShapes);

        Shape[] gOutputShapesForPrediction = new Shape[]{gOutputShapes[0], gOutputShapes[1], gOutputShapes[2]};
        Shape[] gOutputShapesForTimeEvolution = new Shape[]{gOutputShapes[3], gOutputShapes[4], gOutputShapes[5]};



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

        List<OnnxTensor> gOutputForF = List.of(gOutput.get(0), gOutput.get(1), gOutput.get(2));
        List<OnnxTensor> gOutputForG = List.of(gOutput.get(3), gOutput.get(4), gOutput.get(5));




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

    @Override
    public void freeze(boolean[] freeze) {
this.f.freeze(freeze);
this.g.freeze(freeze);
    }
}
