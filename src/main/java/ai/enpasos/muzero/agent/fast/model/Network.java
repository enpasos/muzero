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

package ai.enpasos.muzero.agent.fast.model;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.nn.Block;
import ai.djl.translate.TranslateException;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.agent.fast.model.djl.SubModel;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.cmainfunctions.DynamicsListTranslator;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.cmainfunctions.PredictionListTranslator;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.cmainfunctions.RepresentationListTranslator;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static ai.enpasos.muzero.MuZero.getNetworksBasedir;

@Data
public class Network {

    MuZeroConfig config;
    Model model;
    private SubModel representation;
    private SubModel prediction;
    private SubModel dynamics;

    public Network(@NotNull MuZeroConfig config, @NotNull Model model, Path modelPath) {
        this.model = model;
        this.config = config;
        MuZeroBlock block = new MuZeroBlock(config);


        model.setBlock(block);
        try {
            model.load(modelPath);
        } catch (@NotNull IOException | MalformedModelException e) {
            e.printStackTrace();
        }

        Block representationBlock = model.getBlock().getChildren().get("01Representation");
        Block predictionBlock = model.getBlock().getChildren().get("02Prediction");
        Block dynamicsBlock = model.getBlock().getChildren().get("03Dynamics");


        representation = new SubModel("representation", model, representationBlock);
        prediction = new SubModel("prediction", model, predictionBlock);
        dynamics = new SubModel("dynamics", model, dynamicsBlock);

    }

    public Network(@NotNull MuZeroConfig config, @NotNull Model model) {
        this(config, model, Paths.get(getNetworksBasedir(config)));

    }

    public static double getLoss(@NotNull Model model) {
        double epoch = 0;
        String prop = model.getProperty("MeanLoss");
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

    public NDManager getNDManager() {
        return model.getNDManager();
    }

    public @Nullable NetworkIO representationList(List<Observation> observationList) {
        NetworkIO networkOutputFromRepresentation = null;

        RepresentationListTranslator translator = new RepresentationListTranslator();
        try (Predictor<List<Observation>, NetworkIO> predictorRepresentation = representation.newPredictor(translator)) {
            networkOutputFromRepresentation = predictorRepresentation.predict(observationList);

        } catch (TranslateException e) {
            e.printStackTrace();
        }
        return networkOutputFromRepresentation;
    }

    public @Nullable List<NetworkIO> predictionList(NetworkIO networkio) {
        List<NetworkIO> networkOutputFromPrediction = null;

        PredictionListTranslator translator = new PredictionListTranslator();
        try (Predictor<NetworkIO, List<NetworkIO>> predictorRepresentation = prediction.newPredictor(translator)) {
            networkOutputFromPrediction = predictorRepresentation.predict(networkio);
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        return networkOutputFromPrediction;
    }

    public @Nullable NetworkIO dynamicsList(NetworkIO networkio) {

        NetworkIO networkOutputFromDynamics = null;

        DynamicsListTranslator translator = new DynamicsListTranslator();
        try (Predictor<NetworkIO, NetworkIO> predictorRepresentation = dynamics.newPredictor(translator)) {
            networkOutputFromDynamics = predictorRepresentation.predict(networkio);
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        return networkOutputFromDynamics;
    }

    public NetworkIO initialInference(@NotNull Observation observation) {
        return Objects.requireNonNull(initialInferenceList(List.of(observation))).get(0);
    }

    public @Nullable List<NetworkIO> initialInferenceList(List<Observation> observationList) {
      //  checkObservations(observationList);
        NetworkIO outputA = representationList(observationList);
        List<NetworkIO> outputB = predictionList(outputA);

        for (int i = 0; i < Objects.requireNonNull(outputB).size(); i++) {
            outputB.get(i).setHiddenState(Objects.requireNonNull(outputA).getHiddenState().get(i));
        }

        return outputB;
    }


//    private long currentEpoch = -1;
//    private Set<Observation> knownObservations = new HashSet<>();
//    private int count;
//    private void checkObservations(List<Observation> observationList) {
//        int modelEpoch = getEpoch(this.model);
//        if (currentEpoch != modelEpoch) {  // different model
//            // clear memory
//            knownObservations.clear();
//            currentEpoch = modelEpoch;
//            count = 0;
//        }
//        observationList.stream().forEach(o -> {
//            if (knownObservations.contains(o)) {
//                count++;
//            } else {
//                knownObservations.add(o);
//            }
//        });
//    }

    public NetworkIO recurrentInference(@NotNull NDArray hiddenState, @NotNull NDArray actionList) {
        return Objects.requireNonNull(recurrentInferenceList(List.of(hiddenState), List.of(actionList))).get(0);
    }

    public @Nullable List<NetworkIO> recurrentInferenceList(@NotNull List<NDArray> hiddenStateList, List<NDArray> actionList) {
        NetworkIO networkIO = new NetworkIO();

        networkIO.setHiddenState(NDArrays.stack(new NDList(hiddenStateList)));
        networkIO.setActionList(actionList);

        networkIO.setConfig(config);
        NetworkIO outputA = dynamicsList(networkIO);
        List<NetworkIO> outputB = predictionList(outputA);

        for (int i = 0; i < Objects.requireNonNull(outputB).size(); i++) {
            outputB.get(i).setHiddenState(Objects.requireNonNull(outputA).getHiddenState().get(i));
        }

        return outputB;
    }

    public int trainingSteps() {
        return getEpoch(model) * config.getNumberOfTrainingStepsPerEpoch();
    }
}
