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
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.agent.rational.MCTS;
import ai.enpasos.muzero.platform.agent.rational.MinMaxStats;
import ai.enpasos.muzero.platform.agent.rational.Node;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Map.entry;

@Component
public class Inference {

    @Autowired
    MuZeroConfig config;

    @Autowired
    MCTS mcts;

    public int aiDecision(List<Integer> actions, boolean withMCTS, String networkDir) {
        return aiDecision(actions, withMCTS, networkDir, DeviceType.CPU);
    }


    public int[] aiDecisionForGames(List<Game> games, boolean withMCTS, Map<String, ?> options) {

        int[] actionIndexesSelectedByNetwork;
       // config.setNetworkBaseDir(networkDir);

        try (Model model = Model.newInstance(config.getModelName())) {

            Network network = new Network(config, model, Path.of(config.getNetworkBaseDir()),  options);
            try (NDManager nDManager = network.getNDManager().newSubManager()) {

                network.initActionSpaceOnDevice(nDManager);
                network.setHiddenStateNDManager(nDManager);

                actionIndexesSelectedByNetwork = aiDecision(network, withMCTS, games).stream()
                    .mapToInt(i -> i.getSecond()).toArray();

            }

        }
        return actionIndexesSelectedByNetwork;
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
        //config.setNetworkBaseDir(networkDir);
        //config.setInferenceDeviceType(DeviceType.CPU);
        Game game = config.newGame();
        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {
            Network network = new Network(config, model, Path.of(config.getNetworkBaseDir()),  Map.ofEntries(entry("epoch", epoch + "")));
            try (NDManager nDManager = network.getNDManager().newSubManager()) {
                network.setHiddenStateNDManager(nDManager);
                valueByNetwork = aiDecision(network, false, game).getFirst();
            }
        }
        return valueByNetwork;
    }
    public double aiValue(List<Integer> actions, String networkDir) {
        double valueByNetwork;
        config.setNetworkBaseDir(networkDir);
        config.setInferenceDeviceType(DeviceType.CPU);
        Game game = getGame(actions);
        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {
            Network network = new Network(config, model);
            try (NDManager nDManager = network.getNDManager().newSubManager()) {
                network.setHiddenStateNDManager(nDManager);
                valueByNetwork = aiDecision(network, false, game).getFirst();
            }
        }
        return valueByNetwork;
    }

    public  double[] aiValue(List<Game> games) {
        double[] valueByNetwork;
        try (Model model = Model.newInstance(config.getModelName())) {
            Network network = new Network(config, model);
            valueByNetwork = aiValue( network,  games);
        }
        return valueByNetwork;
    }

    public  double[] aiValue(Network network, List<Game> games) {
        double[] valueByNetwork;
        try (NDManager nDManager = network.getNDManager().newSubManager()) {
            network.setHiddenStateNDManager(nDManager);
            List<NetworkIO> networkOutputs = network.initialInferenceListDirect(games);
            valueByNetwork = networkOutputs.stream().mapToDouble(o -> o.getValue()).toArray();
        }
        return valueByNetwork;
    }


    public Game getGame(List<Integer> actions) {
        Game game = config.newGame();
        actions.forEach(a -> game.apply(config.newAction(a)));
        return game;
    }


    private Pair<Double, Integer> aiDecision(@NotNull Network network, boolean withMCTS, Game game) {
        return aiDecision(network, withMCTS, List.of(game)).get(0);
//        NetworkIO networkOutput = network.initialInferenceDirect(game);
//        double aiValue = networkOutput.getValue();
//        int actionIndexSelectedByNetwork;
//        List<Action> legalActions = game.legalActions();
//        if (!withMCTS) {
//
//            float[] policyValues = networkOutput.getPolicyValues();
//            List<Pair<Action, Double>> distributionInput =
//                    IntStream.range(0, game.getConfig().getActionSpaceSize())
//                            .filter(i -> {
//                                Action action = game.getConfig().newAction(i);
//                                return legalActions.contains(action);
//                            })
//                            .mapToObj(i -> {
//                                Action action = game.getConfig().newAction(i);
//                                double v = policyValues[i];
//                                return new Pair<>(action, v);
//                            }).collect(Collectors.toList());
//
//            Action action = mcts.selectActionByMaxFromDistribution(distributionInput);
//            actionIndexSelectedByNetwork = action.getIndex();
//
//        } else {
//            Node root = new Node(network.getConfig(), 0);
//
//            mcts.expandNode(root, game.toPlay(), legalActions, networkOutput, false);
//
//            MinMaxStats minMaxStats = mcts.run(root, game.actionHistory(), network, null);
//            Action action = mcts.selectActionByMax(root, minMaxStats);
//            actionIndexSelectedByNetwork = action.getIndex();
//
//        }
//        return Pair.create(aiValue, actionIndexSelectedByNetwork);
    }




    private List<Pair<Double, Integer>> aiDecision(@NotNull Network network, boolean withMCTS, List<Game> games) {
        List<NetworkIO> networkOutputList = network.initialInferenceListDirect(games);

        int actionIndexSelectedByNetwork;

        List<Pair<Double, Integer>> result = new ArrayList<>();

        if (!withMCTS) {
            for (int g = 0; g < games.size(); g++) {
                Game game = games.get(g);
                List<Action> legalActions = game.legalActions();
                float[] policyValues = networkOutputList.get(g).getPolicyValues();
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

                Action action = mcts.selectActionByMaxFromDistribution(distributionInput);
                actionIndexSelectedByNetwork = action.getIndex();
                double aiValue = networkOutputList.get(g).getValue();
                result.add(Pair.create(aiValue, actionIndexSelectedByNetwork));
            }

        } else {
            List<Node> rootNodes = IntStream.range(0, games.size())
                .mapToObj(i -> new Node(config, 0, true))
                .collect(Collectors.toList());

            for (int g = 0; g < games.size(); g++) {
                Game game = games.get(g);
                List<Action> legalActions = game.legalActions();
                Node root = new Node(network.getConfig(), 0);
                mcts.expandNode(rootNodes.get(g), game.toPlay(), legalActions, networkOutputList.get(g), false);
            }
            List<MinMaxStats> minMaxStatsList = mcts.runParallel(rootNodes,
                games.stream().map(Game::actionHistory).collect(Collectors.toList()),
                network, null, config.getNumSimulations());
            for (int g = 0; g < games.size(); g++) {
                Action action = mcts.selectActionByMax(rootNodes.get(g), minMaxStatsList.get(g));
                actionIndexSelectedByNetwork = action.getIndex();
                double aiValue = networkOutputList.get(0).getValue();
                result.add(Pair.create(aiValue, actionIndexSelectedByNetwork));
            }
        }
        return result;
    }


}
