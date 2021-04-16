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

package ai.enpasos.muzero.network.djl;


import ai.djl.Model;
import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.listener.TrainingListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A {@link TrainingListener} that saves a model checkpoint after each epoch.
 */
public class MyCheckpointsTrainingListener extends TrainingListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MyCheckpointsTrainingListener.class);

    private final @Nullable String outputDir;
    private final int step;
    private String overrideModelName;
    private Consumer<Trainer> onSaveModel;
    private int epoch;


    public MyCheckpointsTrainingListener(String outputDir) {
        this(outputDir, null, -1);
    }


    public MyCheckpointsTrainingListener(String outputDir, String overrideModelName) {
        this(outputDir, overrideModelName, -1);
    }


    public MyCheckpointsTrainingListener(@Nullable String outputDir, String overrideModelName, int step) {
        this.outputDir = outputDir;
        this.step = step;
        if (outputDir == null) {
            throw new IllegalArgumentException(
                    "Can not save checkpoint without specifying an output directory");
        }
        this.overrideModelName = overrideModelName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEpoch(@NotNull Trainer trainer) {
        setEpoch(getEpoch() + 1);
        if (outputDir == null) {
            return;
        }

        if (step > 0 && getEpoch() % step == 0) {
            // save model at end of each epoch
            saveModel(trainer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTrainingEnd(@NotNull Trainer trainer) {
        if (step == -1 || getEpoch() % step != 0) {
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

    protected void saveModel(@NotNull Trainer trainer) {
        Model model = trainer.getModel();
        String modelName = model.getName();
        if (overrideModelName != null) {
            modelName = overrideModelName;
        }
        try {
            model.setProperty("Epoch", String.valueOf(getEpoch()));
            if (onSaveModel != null) {
                onSaveModel.accept(trainer);
            }
            model.save(Paths.get(Objects.requireNonNull(outputDir)), modelName);
        } catch (IOException e) {
            logger.error("Failed to save checkpoint", e);
        }
    }

    public int getEpoch() {
        return epoch;
    }

    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }
}

