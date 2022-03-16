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

package ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference;

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
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.cmainfunctions.DynamicsBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.cmainfunctions.PredictionBlock;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class RecurrentInferenceBlock extends AbstractBlock implements OnnxIO {

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
        NDList fResult = f.forward(parameterStore, gResult, training);
        return gResult.addAll(fResult);
    }


    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] gOutputShapes = g.getOutputShapes(inputShapes);
        Shape[] fOutputShapes = f.getOutputShapes(gOutputShapes);
        return ArrayUtils.addAll(gOutputShapes, fOutputShapes);
    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {

        g.initialize(manager, dataType, inputShapes);
        Shape[] hOutputShapes = g.getOutputShapes(inputShapes);
        f.initialize(manager, dataType, hOutputShapes);
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

        Shape stateShape = input.get(0).getShape();
        Shape actionShape = input.get(1).getShape();
        Shape[] inputShapes = new Shape[]{stateShape, actionShape};
        Shape[] gOutputShapes = g.getOutputShapes(inputShapes);

        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .build();

        OnnxBlock gOnnx = g.getOnnxBlock(counter, input);
        onnxBlock.addChild(gOnnx);
        List<OnnxTensor> gOutput = gOnnx.getOutput();
        OnnxBlock fOnnx = f.getOnnxBlock(counter, gOutput);
        onnxBlock.addChild(fOnnx);
        List<OnnxTensor> fOutput = fOnnx.getOutput();

        onnxBlock.getValueInfos().addAll(createValueInfoProto(input));

        List<OnnxTensor> totalOutput = new ArrayList<>();
        totalOutput.addAll(gOutput);
        totalOutput.addAll(fOutput);

        onnxBlock.setOutput(totalOutput);

        return onnxBlock;
    }
}
