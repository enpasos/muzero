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


import ai.djl.Model;
import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.listener.TrainingListenerAdapter;
import ai.enpasos.muzero.platform.agent.d_model.Network;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * A {@link TrainingListener} that saves a model checkpoint after each player.
 */
@Component
@Data
@EqualsAndHashCode(callSuper = false)
public class MySaveModelTrainingListener extends TrainingListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MySaveModelTrainingListener.class);
    @Autowired
    GameBuffer gameBuffer;
    private String outputDir;
    private int step;
    private String overrideModelName;
    private Consumer<Trainer> onSaveModel;
    private int epoch;
    private boolean background;

    /**
     * Constructs a {@link MySaveModelTrainingListener} using the model's name.
     *
     * @param outputDir the directory to output the checkpointed models in
     */
    public MySaveModelTrainingListener(String outputDir) {
        this(outputDir, null, -1);
    }

    public MySaveModelTrainingListener() {
        this("outputDir", null, 1);
    }

    public MySaveModelTrainingListener(String outputDir, String overrideModelName, int step) {
        this.outputDir = outputDir;
        this.step = step;
        if (outputDir == null) {
            throw new IllegalArgumentException(
                "Can not save checkpoint without specifying an output directory");
        }
        this.overrideModelName = overrideModelName;
    }

    @Override
    public void onEpoch(Trainer trainer) {
        if (background) return;

        epoch++;
        if (outputDir == null) {
            return;
        }

        if (step > 0 && epoch % step == 0) {
            saveModel(trainer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTrainingEnd(Trainer trainer) {
        if (background) return;
        if (  step == -1 || epoch % step != 0) {
            saveModel(trainer);
        }
    }

    /**
     * Returns the override model name to save checkpoints with.
     *
     * @return the override model name to save checkpoints with
     */
    public String getOverrideModelName() {
        return overrideModelName;
    }

    /**
     * Sets the override model name to save checkpoints with.
     *
     * @param overrideModelName the override model name to save checkpoints with
     */
    public void setOverrideModelName(String overrideModelName) {
        this.overrideModelName = overrideModelName;
    }


    /**
     * Sets the callback function on model saving.
     *
     * <p>This allows user to set custom properties to model metadata.
     *
     * @param onSaveModel the callback function on model saving
     */
    public void setSaveModelCallback(Consumer<Trainer> onSaveModel) {
        this.onSaveModel = onSaveModel;
    }

    protected void saveModel(Trainer trainer) {
        Network.debugDumpFromTrainer(trainer);
        Model model = trainer.getModel();
        String modelName = model.getName();
        if (overrideModelName != null) {
            modelName = overrideModelName;
        }
        try {
            model.setProperty("Epoch", String.valueOf(epoch));
            if (onSaveModel != null) {
                onSaveModel.accept(trainer);
            }
            Path modelPath = Paths.get(outputDir);

            model.save(modelPath, modelName);
        } catch (IOException e) {
            logger.error("Failed to save checkpoint", e);
        }
        Network.debugDumpFromTrainer(trainer);
    }

    public int getEpoch() {
        return epoch;
    }

    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }
}
