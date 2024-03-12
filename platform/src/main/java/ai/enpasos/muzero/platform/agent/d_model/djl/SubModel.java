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

package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.BaseModel;
import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.nn.Block;
import ai.djl.nn.Parameter;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.training.initializer.Initializer;
import ai.djl.util.Pair;
import ai.djl.util.PairList;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;


@Slf4j
public class SubModel extends BaseModel {
    public static final String LOAD_NOT_IMPLEMENTED_ON_SUB_MODEL_USE_MODEL = "load not implemented on SubModel (use Model)";
    private final MuZeroConfig config;
    private NDManager hiddenStateNDManager;
    private Model model;

    public SubModel(String modelName, @NotNull Model model, Block block, MuZeroConfig config) {
        super(modelName);
        super.manager = model.getNDManager();
        this.setModel(model);
        super.setBlock(block);
        this.config = config;
    }


    @Override
    public Trainer newTrainer(TrainingConfig trainingConfig) {
//        try(Trainer trainer = model.newTrainer(trainingConfig)) {
//            // go nothing
//            System.out.println("");
//        }


      //  PairList<Initializer, Predicate<Parameter>> initializer = trainingConfig.getInitializers();
        if (block == null) {
            throw new IllegalStateException(
                    "You must set a block for the model before creating a new trainer");
        }
     //   if (wasLoaded) {

            block.freezeParameters(
                    false,
                    p -> p.getType() != Parameter.Type.RUNNING_MEAN && p.getType() != Parameter.Type.RUNNING_VAR);
     //   }
//        for (Pair<Initializer, Predicate<Parameter>> pair : initializer) {
//            if (pair.getKey() != null && pair.getValue() != null) {
//                block.setInitializer(pair.getKey(), pair.getValue());
//            }
//        }

        return new Trainer(this, trainingConfig);
    }

    @Override
    public void load(Path modelPath) {
        log.error(LOAD_NOT_IMPLEMENTED_ON_SUB_MODEL_USE_MODEL);
        throw new NotImplementedException(LOAD_NOT_IMPLEMENTED_ON_SUB_MODEL_USE_MODEL);
    }

    @Override
    public void load(Path modelPath, String prefix) {
        log.error("load2 is not implemented #### not expected to be called");
        throw new NotImplementedException(LOAD_NOT_IMPLEMENTED_ON_SUB_MODEL_USE_MODEL);
    }

    @Override
    public void load(Path modelPath, String prefix, Map<String, ?> options) {
        log.error("load3 is not implemented #### not expected to be called");
        throw new NotImplementedException(LOAD_NOT_IMPLEMENTED_ON_SUB_MODEL_USE_MODEL);
    }


    @Override
    public void cast(DataType dataType) {
        log.error("cast is not implemented #### not expected to be called");
        throw new NotImplementedException("cast not implemented on SubModel (use Model)");
    }

    @Override
    public void quantize() {
        log.error("quantize is not implemented #### not expected to be called");
        throw new NotImplementedException("quantize not implemented on SubModel (use Model)");
    }

    @Override
    public void close() {
        log.error("close is not implemented #### not expected to be called");
        throw new NotImplementedException("close not implemented on SubModel (use Model)");
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public NDManager getHiddenStateNDManager() {
        return hiddenStateNDManager;
    }

    public void setHiddenStateNDManager(NDManager hiddenStateNDManager) {
        this.hiddenStateNDManager = hiddenStateNDManager;
    }

    public MuZeroConfig getConfig() {
        return config;
    }
}
