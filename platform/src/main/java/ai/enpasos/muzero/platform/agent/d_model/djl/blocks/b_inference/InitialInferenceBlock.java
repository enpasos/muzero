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
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.PredictionBlock;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.RepresentationBlock;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class InitialInferenceBlock extends AbstractBlock implements OnnxIO {

    private final RepresentationBlock h;
    private final PredictionBlock f;

    public InitialInferenceBlock(RepresentationBlock representationBlock, PredictionBlock predictionBlock) {
        super(MYVERSION);

        h = this.addChildBlock("Representation", representationBlock);
        f = this.addChildBlock("Prediction", predictionBlock);
    }

    public RepresentationBlock getH() {
        return h;
    }

    public PredictionBlock getF() {
        return f;
    }

    @Override
    protected NDList forwardInternal(@NotNull ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {
        NDList hResult = h.forward(parameterStore, inputs, training, params);
        NDList fResult = f.forward(parameterStore, hResult, training, params);
        return hResult.addAll(fResult);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] hOutputShapes = h.getOutputShapes(inputShapes);
        Shape[] fOutputShapes = f.getOutputShapes(hOutputShapes);
        return ArrayUtils.addAll(hOutputShapes, fOutputShapes);
    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        h.initialize(manager, dataType, inputShapes);
        Shape[] hOutputShapes = h.getOutputShapes(inputShapes);
        f.initialize(manager, dataType, hOutputShapes);
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


    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {

        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .build();


        OnnxBlock gOnnx = h.getOnnxBlock(counter, List.of(input.get(0)));
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
