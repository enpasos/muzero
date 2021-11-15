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

package ai.enpasos.muzero.platform.agent.fast.model;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.*;
import ai.djl.translate.TranslateException;
import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.fast.model.djl.SubModel;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.binference.InitialInferenceBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.binference.InitialInferenceListTranslator;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.binference.RecurrentInferenceBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.binference.RecurrentInferenceListTranslator;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.cmainfunctions.DynamicsBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.cmainfunctions.PredictionBlock;
import ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.cmainfunctions.RepresentationBlock;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static ai.enpasos.muzero.platform.MuZero.getNetworksBasedir;
import static ai.enpasos.muzero.platform.agent.slow.play.PlayManager.getAllActionsOnDevice;

@Data
public class Network {

    MuZeroConfig config;
    Model model;
    private SubModel representation;
    private SubModel prediction;
    private SubModel dynamics;

    private SubModel initialInference;
    private SubModel recurrentInference;

    private List<NDArray> actionSpaceOnDevice;

    public Network(@NotNull MuZeroConfig config, @NotNull Model model, Path modelPath) {
        this.model = model;
        this.config = config;

        if (model.getBlock() == null) {
            MuZeroBlock block = new MuZeroBlock(config);
            model.setBlock(block);
            try {
                model.load(modelPath);
            } catch (@NotNull IOException | MalformedModelException e) {
                e.printStackTrace();
            }
        }

        actionSpaceOnDevice = getAllActionsOnDevice(config, model.getNDManager());

        RepresentationBlock representationBlock = (RepresentationBlock) model.getBlock().getChildren().get("01Representation");
        PredictionBlock predictionBlock = (PredictionBlock) model.getBlock().getChildren().get("02Prediction");
        DynamicsBlock dynamicsBlock = (DynamicsBlock) model.getBlock().getChildren().get("03Dynamics");


        representation = new SubModel("representation", model, representationBlock);
        prediction = new SubModel("prediction", model, predictionBlock);
        dynamics = new SubModel("dynamics", model, dynamicsBlock);

        initialInference = new SubModel("initialInference", model, new InitialInferenceBlock(representationBlock, predictionBlock));
        recurrentInference = new SubModel("recurrentInference", model, new RecurrentInferenceBlock(dynamicsBlock, predictionBlock));
    }

    public Network(@NotNull MuZeroConfig config, @NotNull Model model) {
        this(config, model, Paths.get(getNetworksBasedir(config)));
    }

    public static double getDoubleValue(@NotNull Model model, String name) {
        double epoch = 0;
        String prop = model.getProperty(name);
        if (prop != null) {
            epoch = Double.parseDouble(prop);
        }
        return epoch;
    }

    public static int getEpoch(@NotNull Model model) {
        int epoch = 0;
        String prop = model.getProperty("Epoch");
        if (prop != null) {
            epoch = Integer.parseInt(prop);
        }
        return epoch;
    }

    public void setHiddenStateNDManager(NDManager hiddenStateNDManager) {

        initialInference.hiddenStateNDManager = hiddenStateNDManager;
        recurrentInference.hiddenStateNDManager = hiddenStateNDManager;
    }

    public NDManager getNDManager() {
        return model.getNDManager();
    }


    public NetworkIO initialInferenceDirect(@NotNull Observation observation) {
        return Objects.requireNonNull(initialInferenceListDirect(List.of(observation))).get(0);
    }


    public @Nullable List<NetworkIO> initialInferenceListDirect(List<Observation> observationList) {

        List<NetworkIO> networkOutputFromInitialInference = null;

        InitialInferenceListTranslator translator = new InitialInferenceListTranslator();
        try (Predictor<List<Observation>, List<NetworkIO>> predictor = initialInference.newPredictor(translator)) {
            networkOutputFromInitialInference = predictor.predict(observationList);

        } catch (TranslateException e) {
            e.printStackTrace();
        }
        return networkOutputFromInitialInference;


    }


    public @Nullable List<NetworkIO> recurrentInferenceListDirect(@NotNull List<NDArray> hiddenStateList, List<NDArray> actionList) {
        NetworkIO networkIO = new NetworkIO();

        networkIO.setHiddenState(NDArrays.stack(new NDList(hiddenStateList)));
        networkIO.setActionList(actionList);

        networkIO.setConfig(config);


        List<NetworkIO> networkOutput = null;

        RecurrentInferenceListTranslator translator = new RecurrentInferenceListTranslator();
        try (Predictor<NetworkIO, List<NetworkIO>> predictorRepresentation = recurrentInference.newPredictor(translator)) {
            networkOutput = predictorRepresentation.predict(networkIO);
        } catch (TranslateException e) {
            e.printStackTrace();
        }


        return networkOutput;
    }

    public int trainingSteps() {
        return getEpoch(model) * config.getNumberOfTrainingStepsPerEpoch();
    }

    public void debugDump() {
        ((BaseNDManager) this.getModel().getNDManager()).debugDump(0);
    }
}
