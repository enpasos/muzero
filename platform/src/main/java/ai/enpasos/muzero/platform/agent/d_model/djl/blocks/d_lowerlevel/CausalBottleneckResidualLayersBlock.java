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

package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel;

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.training.ParameterStore;
import ai.djl.util.Pair;
import ai.djl.util.PairList;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;

public class CausalBottleneckResidualLayersBlock extends AbstractBlock implements OnnxIO, DCLAware {

    public final CausalLayers causalLayers;

    public CausalBottleneckResidualLayersBlock(CausalLayers causalLayers) {
        super(MYVERSION);
        this.causalLayers = addChildBlock("causalBottleneckResidualLayersBlock", causalLayers);
    }

    public CausalBottleneckResidualLayersBlock( int[] numChannels, int[] numCompressedChannels, boolean rescale ) {
        super(MYVERSION);
        if (numChannels.length != numCompressedChannels.length) throw new IllegalArgumentException("num channels and num compressed channels must have the same length");
        List list = new ArrayList();
        for (int i = 0; i < numChannels.length; i++) {
            list.add(new CausalBottleneckResidualBlock(numChannels[i], numChannels[i] / 4 * 3, numCompressedChannels[i], rescale));
        }
        CausalLayers causalLayers = new CausalLayers(list, rescale);
        this.causalLayers = addChildBlock("causalBottleneckResidualLayersBlock", causalLayers);
    }

    @Override
    public @NotNull String toString() {
        return "StartResidual()";
    }

    @Override
    public NDList forward(@NotNull ParameterStore parameterStore, NDList inputs, boolean training) {
        return forward(parameterStore, inputs, training, null);
    }

    @Override
    protected NDList forwardInternal(ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> pairList) {
        return causalLayers.forward(parameterStore, inputs, training);
    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        return causalLayers.getOutputShapes(inputs);

    }

    @Override
    public void initializeChildBlocks(NDManager manager, DataType dataType, Shape... inputShapes) {
        causalLayers.initialize(manager, dataType, inputShapes);
    }

    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        return causalLayers.getOnnxBlock(counter, input);
    }

    @Override
    public void freeze(boolean[] freeze) {
        this.causalLayers.freeze(freeze);
    }





    private  int noOfActiveLayers;

    public int getNoOfActiveLayers() {
        return noOfActiveLayers;
    }

    public void setNoOfActiveLayers(int noOfActiveLayers) {
        this.noOfActiveLayers = noOfActiveLayers;
        this.causalLayers.setNoOfActiveLayers(noOfActiveLayers);
        for (Pair<String, Block> child : this.children) {
            if(child.getValue() instanceof DCLAware dclAware) {
                dclAware.setNoOfActiveLayers(noOfActiveLayers);
            }
        }
    }

    public Block getBlockForInitialRulesOnly() {
        CausalBottleneckResidualLayersBlock block2 = new CausalBottleneckResidualLayersBlock(causalLayers.getBlockForInitialRulesOnly());
        block2.setNoOfActiveLayers(1);
        return block2;
    }
}
