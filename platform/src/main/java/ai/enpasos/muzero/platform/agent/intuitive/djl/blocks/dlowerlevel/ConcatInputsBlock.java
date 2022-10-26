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

package ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.muzero.platform.config.NetworkType;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.createOutput;
import static ai.enpasos.mnist.blocks.OnnxBlock.getNames;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.mnist.blocks.ext.BlocksExt.batchFlatten;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class ConcatInputsBlock extends AbstractBlock implements OnnxIO {

    final NetworkType networkType;

    public ConcatInputsBlock(NetworkType networkType) {
        super(MYVERSION);
        this.networkType = networkType;
    }


    /**
     * @param inputs First input for state, second for action
     */
    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, @NotNull NDList inputs, boolean training, PairList<String, Object> params) {
        NDArray state = inputs.get(0);
        NDArray externalInput = inputs.get(1);
        if (networkType == NetworkType.FC) {
            externalInput =  batchFlatten(externalInput);
        }

        return new NDList(NDArrays.concat(new NDList(state, externalInput), 1));
    }


    @Override
    public Shape[] getOutputShapes(Shape[] inputShapes) {
        long size = 0;
        Shape[] shapes = new Shape[1];
        if (networkType == NetworkType.CON) {
            for (Shape inputShape : inputShapes) {
                size += inputShape.get(1);
            }

            shapes[0] = new Shape(inputShapes[0].get(0), size, inputShapes[0].get(2), inputShapes[0].get(3));
        } else {
            size += inputShapes[0].get(1);
            size += inputShapes[1].get(2) * inputShapes[1].get(3);
            shapes[0] = new Shape(inputShapes[0].get(0), size);
        }

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
