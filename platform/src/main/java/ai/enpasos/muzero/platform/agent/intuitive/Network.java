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

package ai.enpasos.muzero.platform.agent.intuitive;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.training.Trainer;
import ai.djl.translate.TranslateException;
import ai.enpasos.muzero.platform.agent.intuitive.djl.SubModel;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference.InitialInferenceBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference.InitialInferenceListTranslator;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference.RecurrentInferenceBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.binference.RecurrentInferenceListTranslator;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.cmainfunctions.DynamicsBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.cmainfunctions.PredictionBlock;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.cmainfunctions.RepresentationBlock;
import ai.enpasos.muzero.platform.agent.memory.Game;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Data
@Slf4j
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
                log.warn(e.getMessage());
            }
        }

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
        this(config, model, Paths.get(config.getNetworkBaseDir()));
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


    @SuppressWarnings("squid:S125")
    public static void debugDumpFromTrainer(Trainer trainer) {
        //  ((BaseNDManager) trainer.getModel().getNDManager()).debugDump(0);
    }

    public static List<NDArray> getAllActionsOnDevice(MuZeroConfig config, @NotNull NDManager ndManager) {
        List<Action> actions = Objects.requireNonNull(config.newGame()).allActionsInActionSpace();
        return actions.stream().map(action -> action.encode(ndManager)).collect(Collectors.toList());
    }

    public void initActionSpaceOnDevice(NDManager ndManager) {
        actionSpaceOnDevice = getAllActionsOnDevice(config, ndManager);
    }

    public void setHiddenStateNDManager(NDManager hiddenStateNDManager) {
        setHiddenStateNDManager(hiddenStateNDManager, true);
    }

    public void createAndSetHiddenStateNDManager(NDManager parentNDManager, boolean force) {
        if (force || initialInference.getHiddenStateNDManager() == null) {
            NDManager newHiddenStateNDManager;
            if (!MuZeroConfig.HIDDEN_STATE_REMAIN_ON_GPU) {
                newHiddenStateNDManager = parentNDManager.newSubManager(Device.gpu());
            } else {
                newHiddenStateNDManager = parentNDManager.newSubManager(Device.cpu());
            }
            initialInference.setHiddenStateNDManager(newHiddenStateNDManager);
            recurrentInference.setHiddenStateNDManager(newHiddenStateNDManager);
        }
    }

    public void setHiddenStateNDManager(NDManager hiddenStateNDManager, boolean force) {
        if (force || initialInference.getHiddenStateNDManager() == null) {
            initialInference.setHiddenStateNDManager(hiddenStateNDManager);
            recurrentInference.setHiddenStateNDManager(hiddenStateNDManager);
        }
    }

    public NDManager getNDManager() {
        return model.getNDManager();
    }

    public NetworkIO initialInferenceDirect(@NotNull Game game) {
        return Objects.requireNonNull(initialInferenceListDirect(List.of(game))).get(0);
    }

    public @Nullable List<NetworkIO> initialInferenceListDirect(List<Game> gameList) {

        List<NetworkIO> networkOutputFromInitialInference = null;

        InitialInferenceListTranslator translator = new InitialInferenceListTranslator();
        try (Predictor<List<Game>, List<NetworkIO>> predictor = initialInference.newPredictor(translator)) {
            networkOutputFromInitialInference = predictor.predict(gameList);

        } catch (TranslateException e) {
            e.printStackTrace();
        }
        return networkOutputFromInitialInference;


    }

    public @Nullable List<NetworkIO> recurrentInferenceListDirect(@NotNull List<NDArray> hiddenStateList, List<NDArray> actionList) {
        NetworkIO networkIO = new NetworkIO();
        NDArray hiddenState = NDArrays.stack(new NDList(hiddenStateList));
        networkIO.setHiddenState(hiddenState);
        networkIO.setActionList(actionList);

        networkIO.setConfig(config);


        List<NetworkIO> networkOutput = null;

        RecurrentInferenceListTranslator translator = new RecurrentInferenceListTranslator();
        try (Predictor<NetworkIO, List<NetworkIO>> predictorRepresentation = recurrentInference.newPredictor(translator)) {
            networkOutput = predictorRepresentation.predict(networkIO);
        } catch (TranslateException e) {
            e.printStackTrace();
        }

        hiddenState.close();
        return networkOutput;
    }

    public int trainingSteps() {
        return getEpoch(model) * config.getNumberOfTrainingStepsPerEpoch();
    }

    @SuppressWarnings("squid:S125")
    public void debugDump() {
        //   ((BaseNDManager) this.getModel().getNDManager()).debugDump(0);
    }

}
