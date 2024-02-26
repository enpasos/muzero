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

package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions;

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.mnist.blocks.ext.ActivationExt;
import ai.enpasos.mnist.blocks.ext.BlocksExt;
import ai.enpasos.mnist.blocks.ext.LinearExt;
import ai.enpasos.mnist.blocks.ext.SequentialBlockExt;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.CausalityFreezing;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.CausalLayersToPrediction;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.Conv1x1LayerNormRelu;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;

@SuppressWarnings("java:S110")
public class PredictionBlock extends AbstractBlock implements OnnxIO, CausalityFreezing {

    public PredictionBlock(@NotNull MuZeroConfig config ) {
        this(
                config.getPlayerMode() == PlayerMode.TWO_PLAYERS,
                config.getActionSpaceSize() );
    }


    private SequentialBlockExt valueHead;
    private SequentialBlockExt legalActionsHead;
    private SequentialBlockExt policyHead;
    private SequentialBlockExt rewardHead;


 //   private CausalLayersToPrediction causalLayersToPrediction;

    @Setter
    private boolean withReward;





    public PredictionBlock(boolean isPlayerModeTWOPLAYERS, int actionSpaceSize ) {
    //    causalLayersToPrediction = new CausalLayersToPrediction();

        valueHead = new SequentialBlockExt();
        valueHead.add(Conv1x1LayerNormRelu.builder().channels(1).build())
                .add(BlocksExt.batchFlattenBlock())
                .add(LinearExt.builder()
                        .setUnits(256)
                        .build())
                .add(ActivationExt.reluBlock())
                .add(LinearExt.builder()
                        .setUnits(1).build());
        if (isPlayerModeTWOPLAYERS) {
            valueHead.add(ActivationExt.tanhBlock());
        }

        this.addChildBlock("ValueHead", valueHead);


        legalActionsHead = new SequentialBlockExt();
        legalActionsHead.add(Conv1x1LayerNormRelu.builder().channels(1).build())   // 1 channel?
                .add(BlocksExt.batchFlattenBlock())
                .add(LinearExt.builder()
                        .setUnits(actionSpaceSize).build());

        this.addChildBlock("LegalActionsHead", legalActionsHead);


        policyHead = new SequentialBlockExt();
        policyHead
                .add(Conv1x1LayerNormRelu.builder().channels(2).build())
                .add(BlocksExt.batchFlattenBlock())
                .add(LinearExt.builder()
                        .setUnits(actionSpaceSize)
                        .build());

        this.addChildBlock("PolicyHead", policyHead);


        rewardHead = new SequentialBlockExt();

        rewardHead.add(Conv1x1LayerNormRelu.builder().channels(1).build())
                .add(BlocksExt.batchFlattenBlock())
                .add(LinearExt.builder()
                        .setUnits(256)
                        .build())
                .add(ActivationExt.reluBlock())
                .add(LinearExt.builder()
                        .setUnits(1).build());
        if (isPlayerModeTWOPLAYERS) {
            rewardHead.add(ActivationExt.tanhBlock());
        }
        this.addChildBlock("RewardHead", rewardHead);


    }


    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {
        NDList results = new NDList();
    //    inputs = causalLayersToPrediction.forward(parameterStore, inputs, training, params);
        results.add(this.legalActionsHead.forward(parameterStore, new NDList(inputs.get(0)), training, params).get(0));
        if (withReward) {
            results.add(this.rewardHead.forward(parameterStore, new NDList(inputs.get(1)), training, params).get(0));
        }
         results.add(this.policyHead.forward(parameterStore, new NDList(inputs.get(2)), training, params).get(0));
        results.add(this.valueHead.forward(parameterStore, new NDList(inputs.get(3)), training, params).get(0));
        return results;
    }

    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        Shape[] inputShapes_ = inputShapes;//causalLayersToPrediction.getOutputShapes(inputShapes);
        legalActionsHead.initialize(manager, dataType, new Shape[]{inputShapes_[0] });
        if (withReward) {
            rewardHead.initialize(manager, dataType, new Shape[]{inputShapes_[1] });
        }
        policyHead.initialize(manager, dataType, new Shape[]{inputShapes_[2] });
        valueHead.initialize(manager, dataType, new Shape[]{inputShapes_[3] });
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
     //   inputShapes = causalLayersToPrediction.getOutputShapes(inputShapes);
        Shape[] result = new Shape[withReward ? 4 : 3];
        int c = 0;
        result[c++] = this.legalActionsHead.getOutputShapes(new Shape[]{inputShapes[0]})[0];
        if (withReward) {
            result[c++] = this.rewardHead.getOutputShapes(new Shape[]{inputShapes[1]})[0];
        }
        result[c++] = this.policyHead.getOutputShapes(new Shape[]{inputShapes[2]})[0];
        result[c] = this.valueHead.getOutputShapes(new Shape[]{inputShapes[3]})[0];

        return result;
    }



    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        OnnxBlock onnxBlock = OnnxBlock.builder()
                .input(input)
                .valueInfos(createValueInfoProto(input))
                .build();

        List<OnnxTensor> outputs = new ArrayList<>();
        OnnxTensor childOutput = null;

//        OnnxBlock child =   causalLayersToPrediction.getOnnxBlock(counter, input);
//        onnxBlock.addChild(child);
//         input = child.getOutput();


        OnnxBlock   child = this.legalActionsHead.getOnnxBlock(counter,  List.of(input.get(0)));
            onnxBlock.addChild(child);
            childOutput = child.getOutput().get(0);
            outputs.add(childOutput);
            if (withReward) {
                child = this.rewardHead.getOnnxBlock(counter,  List.of(input.get(1)));
                onnxBlock.addChild(child);
                childOutput = child.getOutput().get(0);
                outputs.add(childOutput);
            }


            child = this.policyHead.getOnnxBlock(counter,  List.of(input.get(2)));
            onnxBlock.addChild(child);
            childOutput = child.getOutput().get(0);
            outputs.add(childOutput);

            child = this.valueHead.getOnnxBlock(counter,  List.of(input.get(3)));
            onnxBlock.addChild(child);
            childOutput = child.getOutput().get(0);
            outputs.add(childOutput);


        onnxBlock.setOutput(outputs);

        return onnxBlock;
    }

    @Override
    public void freeze(boolean[] freeze) {
        this.legalActionsHead.freezeParameters(freeze[0]);
        this.rewardHead.freezeParameters(freeze[1]);
        this.policyHead.freezeParameters(freeze[2]);
        this.valueHead.freezeParameters(freeze[3]);
    }
}
