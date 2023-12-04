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
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.*;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;
import static ai.enpasos.mnist.blocks.OnnxBlock.getNames;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class InitialInferenceBlock extends AbstractBlock implements OnnxIO {

    private final Representation1Block h1;
    private final Representation2Block h2;
    private final PredictionBlock f;
    private final LegalActionsBlock f1;
 //   private final RewardBlock f2;

    public InitialInferenceBlock(Representation1Block representation1Block, Representation2Block representation2Block, PredictionBlock predictionBlock, LegalActionsBlock legalActionsBlock ) {
        super(MYVERSION);

        h1 = this.addChildBlock("Representation1", representation1Block);
        h2 = this.addChildBlock("Representation2", representation2Block);
        f = this.addChildBlock("Prediction", predictionBlock);
        f1 = this.addChildBlock("LegalActions", legalActionsBlock);
      //  f2 = this.addChildBlock("Reward", rewardBlock);
    }

    public Representation1Block getH1() {
        return h1;
    }
    public Representation2Block getH2() {
        return h2;
    }
//    public RewardBlock getf2() {
//        return f2;
//    }
    public LegalActionsBlock getf1() {
        return f1;
    }

    public PredictionBlock getF() {
        return f;
    }

    @Override
    protected NDList forwardInternal(@NotNull ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {
        NDList h1Result = h1.forward(parameterStore, inputs, training, params);
        NDList h2Result = h2.forward(parameterStore, h1Result, training, params);
        NDList f1Result = f1.forward(parameterStore, h1Result, training, params);
        NDList fResult = f.forward(parameterStore, h2Result, training, params);
        return h1Result.addAll(f1Result).addAll(fResult);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        Shape[] h1OutputShapes = h1.getOutputShapes(inputShapes);
        Shape[] h2OutputShapes = h2.getOutputShapes(h1OutputShapes);
        Shape[] fOutputShapes = f.getOutputShapes(h2OutputShapes);
        Shape[] f1OutputShapes = f1.getOutputShapes(h1OutputShapes);
        return new Shape[] {  h1OutputShapes[0], f1OutputShapes[0], fOutputShapes[0], fOutputShapes[1] };
    }


    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        h1.initialize(manager, dataType, inputShapes[0]);


        Shape[] state1OutputShapes = h1.getOutputShapes(new Shape[]{inputShapes[0]});
        f1.initialize(manager, dataType, state1OutputShapes[0]);


        h2.initialize(manager, dataType,  state1OutputShapes[0]);
        Shape[] state2OutputShapes = h2.getOutputShapes(state1OutputShapes);
        f.initialize(manager, dataType, state2OutputShapes[0]);


//        similarityProjectorBlock.initialize(manager, dataType, state1OutputShapes[0]);
//        Shape[] projectorOutputShapes = similarityProjectorBlock.getOutputShapes(new Shape[]{state1OutputShapes[0]});
//        similarityPredictorBlock.initialize(manager, dataType, projectorOutputShapes[0]);

//        Shape state1Shape = state1OutputShapes[0];
//        Shape actionShape = inputShapes[1];
//        dynamicsBlock.initialize(manager, dataType, state1Shape, actionShape);

      //  f2.initialize(manager, dataType, state1Shape, actionShape);
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

        int concatDim = 1;
        Shape inputShape = input.get(0).getShape();

        List<OnnxTensor> concatOutput = combine(List.of("T" + counter.count()), List.of(inputShape));

        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .build();


        OnnxBlock h1Onnx = h1.getOnnxBlock(counter, List.of(input.get(0)));
        onnxBlock.addChild(h1Onnx);
        OnnxBlock h2Onnx = h2.getOnnxBlock(counter, h1Onnx.getOutput());
        onnxBlock.addChild(h2Onnx);

        List<OnnxTensor> h1Output = h1Onnx.getOutput();
        OnnxBlock f1Onnx = f1.getOnnxBlock(counter, h1Output);
        onnxBlock.addChild(f1Onnx);

        List<OnnxTensor> h2Output = h2Onnx.getOutput();
        OnnxBlock fOnnx = f.getOnnxBlock(counter, h2Output);
        onnxBlock.addChild(fOnnx);




        List<OnnxTensor> fOutput = fOnnx.getOutput();
        List<OnnxTensor> f1Output = f1Onnx.getOutput();

        onnxBlock.getValueInfos().addAll(createValueInfoProto(input));
        onnxBlock.getValueInfos().addAll(createValueInfoProto(concatOutput));

        List<OnnxTensor> totalOutput = new ArrayList<>();
        totalOutput.addAll(h1Output);
        totalOutput.addAll(f1Output);
        totalOutput.addAll(fOutput);

        onnxBlock.setOutput(totalOutput);

        return onnxBlock;
    }
}
