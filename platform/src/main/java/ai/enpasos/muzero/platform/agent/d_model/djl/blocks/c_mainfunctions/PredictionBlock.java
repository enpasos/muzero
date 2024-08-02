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

import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.DCLAware;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.MySequentialBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
public class PredictionBlock extends MySequentialBlock implements OnnxIO, DCLAware {


    private PredictionBlock() {
        super();
    }


    private PredictionHeads predictionHeads;


    public   @NotNull PredictionBlock(MuZeroConfig config) {
       // PredictionBlock block = new PredictionBlock();
        this();
        predictionHeads = new PredictionHeads(config);
        this
                .add(DynamicsBlock.newDynamicsBlock(config, config.getPrediction()))
                .add(getPredictionHeads());

    }

    @Override
    public void freezeParameters(boolean[] freeze) {
        this.getChildren().forEach(b -> {
            if (b.getValue() instanceof DCLAware) {
                ((DCLAware) b.getValue()).freezeParameters(freeze);
            }
        });
    }

    @Override
    public void setExportFilter(boolean[] exportFilter) {
        this.getChildren().forEach(b -> {
            if (b.getValue() instanceof DCLAware) {
                ((DCLAware) b.getValue()).setExportFilter(exportFilter);
            }
        });
    }


    public PredictionHeads getPredictionHeads() {
        return predictionHeads;
    }
}
