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

package ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.nn.Block;
import ai.djl.training.ParameterStore;
import ai.djl.util.Pair;
import ai.djl.util.PairList;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.c_mainfunctions.*;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import static ai.enpasos.mnist.blocks.OnnxHelper.createValueInfoProto;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock.firstHalf;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock.secondHalf;
import static ai.enpasos.muzero.platform.agent.d_model.djl.blocks.b_inference.InitialInferenceBlock.*;
import static ai.enpasos.muzero.platform.common.Constants.MYVERSION;


public class InitialRulesBlock extends MySequentialBlock implements DCLAware {

    private  int noOfActiveLayers = 1;

    @Override
    public void freeze(boolean[] freeze) {

    }

    public int getNoOfActiveLayers() {
        return noOfActiveLayers;
    }

    public void setNoOfActiveLayers(int noOfActiveLayers) {
        this.noOfActiveLayers = noOfActiveLayers;
        for (Pair<String, Block> child : this.children) {
            if(child.getValue() instanceof DCLAware dclAware) {
                dclAware.setNoOfActiveLayers(noOfActiveLayers);
            }
        }
    }

    private final RepresentationBlock representationBlock;
    private final PredictionBlock predictionBlock;


    public InitialRulesBlock(RepresentationBlock representationBlock, PredictionBlock predictionBlock, DynamicsBlock dynamicsBlock, MuZeroConfig config) {


        this.representationBlock = this.addChildBlock("Representation", representationBlock);
        this.predictionBlock = this.addChildBlock("Prediction", predictionBlock);

    }


    @Override
    protected NDList forwardInternal(@NotNull ParameterStore parameterStore, NDList inputs, boolean training, PairList<String, Object> params) {



        // initial Inference
        predictionBlock.setHeadUsage(new boolean[]{true, false, false, false});
        representationBlock.setNoOfActiveLayers(1);

        return super.forwardInternal( parameterStore,  inputs,  training,   params);

    }



}
