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
import ai.enpasos.mnist.blocks.OnnxBlockExt;
import ai.enpasos.mnist.blocks.OnnxContext;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.onnx.AttributeProto;
import ai.enpasos.onnx.NodeProto;
import ai.enpasos.onnx.TensorProto;

import java.util.List;

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
    public OnnxBlockExt getOnnxBlockExt(OnnxContext ctx) {
        OnnxBlockExt onnxBlockExt = new OnnxBlockExt();

        String inputName =  ctx.getInputNames().get(0);
        Shape  inputShape = ctx.getInputShapes().toArray(new Shape[0])[0];

        Shape shape2 = new Shape(inputShape.get(0), inputShape.get(1) * inputShape.get(2) * inputShape.get(3));
        Shape shape3 = new Shape(inputShape.get(0), 1, 1, 1);


         // NDArray current2 = current.reshape(shape2);

//        String outputShape3 = "Shape3Output" + ctx.counter();
//        String parameterShape3 = "Shape3Parameter" + ctx.counter();
//        onnxBlockExt.getNodes().add(NodeProto.newBuilder()
//                .setName("Node" + ctx.counter())
//                .setOpType("Reshape")
//                .addInput(inputName)
//                .addInput(parameterShape3)
//                .addOutput(outputShape3)
//                .build());
//        long size = ctx.getInputShapes().get(0).size();
//        onnxBlockExt.getParameters().add(TensorProto.newBuilder()
//                .setName(parameterShape3)
//                .setDataType(TensorProto.INT64_DATA_FIELD_NUMBER)
//                .addAllDims(List.of(2L))
//                .addAllInt64Data(convert(shape2.getShape()))
//                .build());
//        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputShape3, shape2));


        String outputMinName = "MinOutput" + + ctx.counter();
        onnxBlockExt.getNodes().add(NodeProto.newBuilder()
                .setName("Node" + ctx.counter())
                .setOpType("ReduceMin")
                .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("axes")
                        .addAllInts(List.of(1L,2L,3L))
                        .build())
                .addInput(inputName)
                .addOutput(outputMinName)
                .build());
        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputMinName, shape3));

        String outputMaxName = "MaxOutput" + + ctx.counter();
        onnxBlockExt.getNodes().add(NodeProto.newBuilder()
                .setName("Node" + ctx.counter())
                .setOpType("ReduceMax")
                .addAttribute(AttributeProto.newBuilder()
                        .setType(AttributeProto.AttributeType.INTS)
                        .setName("axes")
                        .addAllInts(List.of(1L,2L,3L))
                        .build())
                .addInput(inputName)
                .addOutput(outputMaxName)
                .build());
        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputMaxName, shape3));

        String outputASubName = "SubAOutput" + + ctx.counter();
        onnxBlockExt.getNodes().add(NodeProto.newBuilder()
                .setName("Node" + ctx.counter())
                .setOpType("Sub")
                .addInput(inputName)
                .addInput(outputMinName)
                .addOutput(outputASubName)
                .build());
        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputASubName, inputShape));


        String outputBSubName = "SubBOutput" + + ctx.counter();
        onnxBlockExt.getNodes().add(NodeProto.newBuilder()
                .setName("Node" + ctx.counter())
                .setOpType("Sub")
                .addInput(outputMaxName)
                .addInput(outputMinName)
                .addOutput(outputBSubName)
                .build());
        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputBSubName, shape3));

        String outputDivName = "DivOutput" + + ctx.counter();
        onnxBlockExt.getNodes().add(NodeProto.newBuilder()
                .setName("Node" + ctx.counter())
                .setOpType("Div")
                .addInput(outputASubName)
                .addInput(outputBSubName)
                .addOutput(outputDivName)
                .build());
        onnxBlockExt.getValueInfos().add(createValueInfoProto(outputDivName, inputShapes[0]));

        onnxBlockExt.setOutputShapes(List.of(this.getOutputShapes(inputShapes)));
        onnxBlockExt.getOutputNames().add(outputDivName);

        return onnxBlockExt;
    }
}
