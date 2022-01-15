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

package ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel;

import ai.djl.ndarray.NDArrays;
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
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.cmainfunctions.DynamicsBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.cmainfunctions.PredictionBlock;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.*;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class ConcatInputsBlock extends AbstractBlock implements OnnxIO {

    public  ConcatInputsBlock() {
        super(MYVERSION);
    }



    /**
     * @param inputs First input for state, second for action
     */
    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, @NotNull NDList inputs, boolean training, PairList<String, Object> params) {
         return new NDList(NDArrays.concat(inputs, 1));
    }


        @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
            long size = 0;
            for(int i = 0; i < inputShapes.length; i++) {
                size += inputShapes[i].get(1);
            }
            Shape[] shapes = new Shape[1];
            shapes[0] = new Shape(inputShapes[0].get(0), size, inputShapes[0].get(2), inputShapes[0].get(3));

        return shapes;

    }


    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {

        List<OnnxTensor> output = createOutput(List.of("T" + counter.count()), input, this::getOutputShapes);

        int concatDim = 1;

        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .output(output)
            .build();

        onnxBlock.getNodes().add(
            NodeProto.newBuilder()
                .setName("N" + counter.count())
                .setOpType("Concat")
                .addAttribute(AttributeProto.newBuilder()
                    .setType(AttributeProto.AttributeType.INT)
                    .setName("axis")
                    .setI(concatDim)
                    .build())
                .addAllInput(getNames(input))
                .addOutput(output.get(0).getName())
                .build()
        );

        onnxBlock.getValueInfos().addAll(createValueInfoProto(input));
        onnxBlock.getValueInfos().addAll(createValueInfoProto(output));

        return onnxBlock;
    }
}
