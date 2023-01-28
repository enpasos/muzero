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

import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
import ai.enpasos.muzero.platform.config.DeviceType;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.common.Functions.entropy;
import static ai.enpasos.muzero.platform.common.Functions.selectActionByMaxFromDistribution;
import static ai.enpasos.muzero.platform.common.Functions.toDouble;
import static java.util.Map.entry;

@Component
public class Inference {

    @Autowired
    MuZeroConfig config;


    @Autowired
    SelfPlay selfPlay;

    public int aiDecision(List<Integer> actions, boolean withMCTS, String networkDir) {
        return aiDecision(actions, withMCTS, networkDir, DeviceType.CPU);
    }


    public int aiDecisionForGame(List<Integer> actions, boolean withMCTS, Map<String, ?> options) {

        Game game = getGame(actions);

        int[] actionIndexesSelectedByNetwork;

        try (Model model = Model.newInstance(config.getModelName())) {

            Network network = new Network(config, model, Path.of(config.getNetworkBaseDir()), options);

            try (NDManager nDManager = network.getNDManager().newSubManager()) {

                network.initActionSpaceOnDevice(nDManager);
                network.setHiddenStateNDManager(nDManager);

                actionIndexesSelectedByNetwork = aiDecision(network, withMCTS, true, List.of(game)).stream()
                    .mapToInt(Pair::getSecond).toArray();

            }

        }
        return actionIndexesSelectedByNetwork[0];
    }

    public int[] aiDecisionForGames(List<Game> games, boolean withMCTS, Map<String, ?> options) {
        return aiDecisionForGames(games, withMCTS, true, options);
    }

    public int[] aiDecisionForGames(List<Game> games, boolean withMCTS, boolean withRandomness, Map<String, ?> options) {

        int[] actionIndexesSelectedByNetwork;

        try (Model model = Model.newInstance(config.getModelName())) {

            Network network = new Network(config, model, Path.of(config.getNetworkBaseDir()), options);

            try (NDManager nDManager = network.getNDManager().newSubManager()) {

                network.initActionSpaceOnDevice(nDManager);
                network.setHiddenStateNDManager(nDManager);

                actionIndexesSelectedByNetwork = aiDecision(network, withMCTS, withRandomness, games).stream()
                    .mapToInt(Pair::getSecond).toArray();

            }

        }
        return actionIndexesSelectedByNetwork;
    }

    public double[][] getInMindValues(int epoch, int[] actions, int extra, int actionspace) {
        try (Model model = Model.newInstance(config.getModelName())) {
            Network network = new Network(config, model, Path.of(config.getNetworkBaseDir()), Map.ofEntries(entry("epoch", epoch + "")));
            try (NDManager nDManager = network.getNDManager().newSubManager()) {
                network.initActionSpaceOnDevice(nDManager);
                network.setHiddenStateNDManager(nDManager);
                return getInMindValues(network, actions, extra, actionspace);
            }
        }
    }


    private double[][] getInMindValues(Network network, int[] actions, int extra, int actionspace) {
        double[][] values = new double[actions.length + 1][actions.length + 1 + extra];
        Game game = config.newGame();
        for (int t = 0; t <= actions.length; t++) {
            NetworkIO infResult = network.initialInferenceDirect(game);
            NDArray s = infResult.getHiddenState();
            values[actions.length][t] = infResult.getValue();
            System.arraycopy(values[actions.length], 0, values[t], 0, t + 1);
            for (int r = t; r < actions.length + extra; r++) {
                int action;
                if (r < actions.length) {
                    action = actions[r];
                } else {
                    action = ThreadLocalRandom.current().nextInt(actionspace);
                }
                infResult = network.recurrentInference(s, action);
                s = infResult.getHiddenState();
                values[t][r + 1] = infResult.getValue();
            }
            if (t < actions.length) game.apply(actions[t]);
        }
        return values;
    }


    public int aiDecision(List<Integer> actions, boolean withMCTS, String networkDir, DeviceType deviceType) {

        int actionIndexSelectedByNetwork;
        config.setNetworkBaseDir(networkDir);
        config.setInferenceDeviceType(deviceType);
        Game game = getGame(actions);

        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {

            Network network = new Network(config, model);
            try (NDManager nDManager = network.getNDManager().newSubManager()) {

                network.initActionSpaceOnDevice(nDManager);
                network.setHiddenStateNDManager(nDManager);

                actionIndexSelectedByNetwork = aiDecision(network, withMCTS, game).getSecond();
            }

        }
        return actionIndexSelectedByNetwork;
    }

    public double aiStartValue(int epoch) {
        double valueByNetwork;
        Game game = config.newGame();
        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {
            Network network = new Network(config, model, Path.of(config.getNetworkBaseDir()), Map.ofEntries(entry("epoch", epoch + "")));
            try (NDManager nDManager = network.getNDManager().newSubManager()) {
                network.setHiddenStateNDManager(nDManager);
                valueByNetwork = aiDecision(network, false, game).getFirst();
            }
        }
        return valueByNetwork;
    }

    public double aiEntropy(List<Integer> actions, String networkDir) {
        config.setNetworkBaseDir(networkDir);
        config.setInferenceDeviceType(DeviceType.CPU);
        Game game = getGame(actions);
        return aiEntropy(List.of(game))[0];
    }

    public double aiValue(List<Integer> actions, String networkDir) {
        config.setNetworkBaseDir(networkDir);
        config.setInferenceDeviceType(DeviceType.CPU);
        Game game = getGame(actions);
        return aiValue(List.of(game))[0];
    }

    public double[] aiEntropy(List<Game> games) {
        double[] valueByNetwork;
        try (Model model = Model.newInstance(config.getModelName())) {
            Network network = new Network(config, model);
            valueByNetwork = aiEntropy(network, games);
        }
        return valueByNetwork;
    }

    public double[] aiValue(List<Game> games) {
        double[] valueByNetwork;
        try (Model model = Model.newInstance(config.getModelName())) {
            Network network = new Network(config, model);
            valueByNetwork = aiValue(network, games);
        }
        return valueByNetwork;
    }

    public double[] aiValue(Network network, List<Game> games) {
        double[] valueByNetwork;
        try (NDManager nDManager = network.getNDManager().newSubManager()) {
            network.setHiddenStateNDManager(nDManager);
            List<NetworkIO> networkOutputs = network.initialInferenceListDirect(games);
            valueByNetwork = Objects.requireNonNull(networkOutputs).stream().mapToDouble(NetworkIO::getValue).toArray();
        }
        return valueByNetwork;
    }

    public double[] aiEntropy(Network network, List<Game> games) {
        double[] entropyByNetwork;
        try (NDManager nDManager = network.getNDManager().newSubManager()) {
            network.setHiddenStateNDManager(nDManager);
            List<NetworkIO> networkOutputs = network.initialInferenceListDirect(games);
            entropyByNetwork = Objects.requireNonNull(networkOutputs).stream().mapToDouble(io -> entropy(toDouble(io.getPolicyValues()))).toArray();
        }
        return entropyByNetwork;
    }


    public Game getGame(List<Integer> actions) {
        Game game = config.newGame();
        actions.forEach(a -> game.apply(config.newAction(a)));
        return game;
    }


    private Pair<Double, Integer> aiDecision(@NotNull Network network, boolean withMCTS, Game game) {
        return aiDecision(network, withMCTS, true, List.of(game)).get(0);
    }


    @SuppressWarnings("java:S1135")
    private List<Pair<Double, Integer>> aiDecision(@NotNull Network network, boolean withMCTS, boolean withRandomness, List<Game> gamesInput) {


        List<Game> games = new ArrayList<>();
        for (Game game : gamesInput) {
            games.add(game.copy());
        }


        List<NetworkIO> networkOutputList = network.initialInferenceListDirect(games);


        int actionIndexSelectedByNetwork;

        List<Pair<Double, Integer>> result = new ArrayList<>();

        if (!withMCTS) {
            for (int g = 0; g < games.size(); g++) {
                Game game = games.get(g);
                List<Action> legalActions = game.legalActions();
                float[] policyValues = Objects.requireNonNull(networkOutputList).get(g).getPolicyValues();
                List<Pair<Action, Double>> distributionInput =
                    IntStream.range(0, game.getConfig().getActionSpaceSize())
                        .filter(i -> {
                            Action action = game.getConfig().newAction(i);
                            return legalActions.contains(action);
                        })
                        .mapToObj(i -> {
                            Action action = game.getConfig().newAction(i);
                            double v = policyValues[i];
                            return new Pair<>(action, v);
                        }).collect(Collectors.toList());

                Action action = selectActionByMaxFromDistribution(distributionInput);
                actionIndexSelectedByNetwork = action.getIndex();
                double aiValue = networkOutputList.get(g).getValue();
                result.add(Pair.create(aiValue, actionIndexSelectedByNetwork));
            }

        } else {
            // TODO: needs to be tested

            selfPlay.init(games);
            selfPlay.play(network, false, false, false, false,   0, false);
            List<Action> actions = games.stream().map(g -> g.actionHistory().lastAction()).collect(Collectors.toList());

            for (int g = 0; g < games.size(); g++) {
                Action action = actions.get(g);
                actionIndexSelectedByNetwork = action.getIndex();
                double aiValue = Objects.requireNonNull(networkOutputList).get(0).getValue();
                result.add(Pair.create(aiValue, actionIndexSelectedByNetwork));
            }
        }
        return result;
    }



}
