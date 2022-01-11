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

package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;

import java.util.List;

import static ai.enpasos.mnist.blocks.OnnxBlock.combine;
import static ai.enpasos.mnist.blocks.OnnxHelper.convert;
import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;


public class RescaleBlockExt extends AbstractBlock implements OnnxIO {


    public RescaleBlockExt() {
        super();
    }


    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {
        NDArray current = inputs.head();

        // Scale to the range [0, 1]  (same range as the action input)
        Shape origShape = current.getShape();
        Shape shape2 = new Shape(origShape.get(0), origShape.get(1) * origShape.get(2) * origShape.get(3));
        NDArray current2 = current.reshape(shape2);
        Shape shape3 = new Shape(origShape.get(0), 1, 1, 1);
        NDArray min2 = current2.min(new int[]{1}, true).reshape(shape3);
        NDArray max2 = current2.max(new int[]{1}, true).reshape(shape3);

        NDArray d = max2.sub(min2).maximum(1e-5);

        NDArray a = current.sub(min2);
        return new NDList(a.div(d));

    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        return inputs;
    }



    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {

        OnnxBlock blockMin = nodeMin(counter, input);
        OnnxBlock blockMax = nodeMax(counter, input);
        OnnxBlock blockSubA = nodeSub(counter, input, blockMin.getOutput());
        OnnxBlock blockSubB = nodeSub(counter, blockMax.getOutput(), blockMin.getOutput());
        OnnxBlock blockDiv = nodeDiv(counter, blockSubA.getOutput(), blockSubB.getOutput());

        OnnxBlock onnxBlock = OnnxBlock.builder()
            .input(input)
            .output( blockDiv.getOutput())
            .valueInfos(createValueInfoProto( blockDiv.getOutput()))
            .build();

        onnxBlock.addChild(blockMin);
        onnxBlock.addChild(blockMax);
        onnxBlock.addChild(blockSubA);
        onnxBlock.addChild(blockSubB);
        onnxBlock.addChild(blockDiv);

        return onnxBlock;

    }

    private OnnxBlock nodeMin(OnnxCounter counter, List<OnnxTensor> input) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(new Shape(input.get(0).getShape().get(0), 1, 1, 1))
        );
        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("ReduceMin")
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("axes")
                        .addAllInts(List.of(1L,2L,3L))
                        .build())
                    .addInput(input.get(0).getName())
                    .addOutput(output.get(0).getName())
                    .build())
            )
            .build();
     }
    private OnnxBlock nodeMax(OnnxCounter counter, List<OnnxTensor> input) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(new Shape(input.get(0).getShape().get(0), 1, 1, 1))
        );
        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("ReduceMax")
                    .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("axes")
                        .addAllInts(List.of(1L,2L,3L))
                        .build())
                    .addInput(input.get(0).getName())
                    .addOutput(output.get(0).getName())
                    .build())
            )
            .build();
    }
    private OnnxBlock nodeSub(OnnxCounter counter, List<OnnxTensor> inputA, List<OnnxTensor> inputB) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(inputA.get(0).getShape())
        );
        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("Sub")
                    .addInput(inputA.get(0).getName())
                    .addInput(inputB.get(0).getName())
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .valueInfos(createValueInfoProto(output))
            .build();
    }
    private OnnxBlock nodeDiv(OnnxCounter counter, List<OnnxTensor> inputA, List<OnnxTensor> inputB) {
        List<OnnxTensor> output = combine(
            List.of("T" + counter.count()),
            List.of(inputA.get(0).getShape())
        );
        return OnnxBlock.builder()
            .output(output)
            .valueInfos(createValueInfoProto(output))
            .nodes(List.of(
                NodeProto.newBuilder()
                    .setName("N" + counter.count())
                    .setOpType("Div")
                    .addInput(inputA.get(0).getName())
                    .addInput(inputB.get(0).getName())
                    .addOutput(output.get(0).getName())
                    .build()
            ))
            .valueInfos(createValueInfoProto(output))
            .build();
    }
}
