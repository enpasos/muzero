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

package ai.enpasos.muzero.platform.agent.slow.play;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class MCTS {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private RegularizedPolicyOptimization regularizedPolicyOptimization;

    private @NotNull RandomGenerator rng;

    public MCTS() {
        this.rng = new Well19937c();
    }


    public void backUp(@NotNull List<Node> searchPath, double value, Player toPlay, double discount, @NotNull MinMaxStats minMaxStats) {
        for (int i = searchPath.size() - 1; i >= 0; i--) {
            Node node = searchPath.get(i);
            if (node.getToPlay() == toPlay) {
                node.setValueSum(node.getValueSum() + value);
            } else {
                node.setValueSum(node.getValueSum() - value);
            }
            node.setVisitCount(node.getVisitCount() + 1);
            value = node.getReward() + discount * value;
            minMaxStats.update(value);
        }
    }

    public MinMaxStats run(@NotNull Node root, @NotNull ActionHistory actionHistory, @NotNull Network network,
                           Duration inferenceDuration) {

        return runParallel(List.of(root), List.of(actionHistory), network, inferenceDuration, config.getNumSimulations()).get(0);
    }

    public List<MinMaxStats> runParallel(@NotNull List<Node> rootList, @NotNull List<ActionHistory> actionHistoryList, @NotNull Network network,
                                         @Nullable Duration inferenceDuration, int numSimulations) {

        List<MinMaxStats> minMaxStatsList = IntStream.range(0, rootList.size())
                .mapToObj(i -> new MinMaxStats(config.getKnownBounds()))
                .collect(Collectors.toList());


        for (int i = 0; i < numSimulations; i++) {

            List<ActionHistory> historyList = actionHistoryList.stream().map(ActionHistory::copy).collect(Collectors.toList());
            List<Node> nodeList = new ArrayList<>(rootList);
            List<List<Node>> searchPathList = IntStream.range(0, rootList.size())
                    .mapToObj(nL -> new ArrayList<Node>())
                    .collect(Collectors.toList());

            search(minMaxStatsList, historyList, nodeList, searchPathList);

            expandAndBackpropagate(network, inferenceDuration, minMaxStatsList, historyList, nodeList, searchPathList);

        }

        clean(rootList);
        return minMaxStatsList;
    }

    private void expandAndBackpropagate(@NotNull Network network, @Nullable Duration inferenceDuration, List<MinMaxStats> minMaxStatsList, List<ActionHistory> historyList, List<Node> nodeList, List<List<Node>> searchPathList) {
        List<Action> lastActions = historyList.stream().map(ActionHistory::lastAction).collect(Collectors.toList());
        List<NDArray> actionList = lastActions.stream().map(action ->
                network.getActionSpaceOnDevice().get(action.getIndex())
        ).collect(Collectors.toList());

        if (inferenceDuration != null) inferenceDuration.value -= System.currentTimeMillis();
        List<NetworkIO> networkOutputList = recurrentInference(network, searchPathList, actionList);
        if (inferenceDuration != null) inferenceDuration.value += System.currentTimeMillis();

        for (int g = 0; g < nodeList.size(); g++) {
            ActionHistory history = historyList.get(g);
            Node node = nodeList.get(g);
            NetworkIO networkOutput = Objects.requireNonNull(networkOutputList).get(g);
            List<Node> searchPath = searchPathList.get(g);
            MinMaxStats minMaxStats = minMaxStatsList.get(g);

            expandNode(node, history.toPlay(), ActionHistory.actionSpace(config), networkOutput, false);

            backUp(searchPath, networkOutput.getValue(), history.toPlay(), config.getDiscount(), minMaxStats);

        }
    }

    @Nullable
    private List<NetworkIO> recurrentInference(@NotNull Network network, List<List<Node>> searchPathList, List<NDArray> actionList) {
        List<NDArray> hiddenStateList = searchPathList.stream().map(searchPath -> {
            Node parent = searchPath.get(searchPath.size() - 2);
            return parent.getHiddenState();
        }).collect(Collectors.toList());
        return network.recurrentInferenceListDirect(hiddenStateList, actionList);
    }

    private void search(List<MinMaxStats> minMaxStatsList, List<ActionHistory> historyList, List<Node> nodeList, List<List<Node>> searchPathList) {
        for (int g = 0; g < nodeList.size(); g++) {
            Node node = nodeList.get(g);
            List<Node> searchPath = searchPathList.get(g);
            MinMaxStats minMaxStats = minMaxStatsList.get(g);
            ActionHistory history = historyList.get(g);
            searchPath.add(node);
            while (node.expanded()) {
                Map.Entry<Action, Node> actionNodeEntry = selectChild(node, minMaxStats);
                Action action = actionNodeEntry.getKey();
                history.addAction(action);
                node = actionNodeEntry.getValue();
                nodeList.set(g, node);
                node.setAction(action);// for debugging
                searchPath.add(node);
            }
        }
    }

    private void clean(@NotNull List<Node> rootList) {
        rootList.forEach(
                this::clean
        );
    }

    private void clean(@NotNull Node node) {
        if (node.getHiddenState() != null) {
            node.getHiddenState().close();
            node.setHiddenState(null);
        }
        node.getChildren().forEach((key, child) -> clean(child));
    }

    private @NotNull String searchPathToString(@NotNull List<Node> searchPath, boolean withValue, MinMaxStats minMaxStats) {
        StringBuilder buf = new StringBuilder();
        searchPath.forEach(
                n -> {
                    if (n.getAction() != null) {
                        buf.append(", ")
                                .append(n.getAction().getIndex());
                        if (withValue) {
                            buf.append("(")
                                    .append(n.valueScore(minMaxStats, config))
                                    .append(")");
                        }
                    } else {
                        buf.append("root");
                    }
                }
        );
        return buf.toString();
    }

    public void expandNode(@NotNull Node node, Player toPlay, @NotNull List<Action> actions, NetworkIO networkOutput,
                           boolean fastRuleLearning) {
        node.setToPlay(toPlay);
        if (!fastRuleLearning) {
            node.setHiddenState(networkOutput.getHiddenState());
            node.setReward(networkOutput.getReward());
        }
        if (fastRuleLearning) {
            double p = 1d / actions.size();
            for (Action action : actions) {
                node.getChildren().put(action, new Node(config, p));  // p/policySum = probability that this action is chosen
            }
        } else {
            Map<Action, Float> policy = actions.stream()
                    .collect(Collectors.toMap(a -> a, a -> networkOutput.getPolicyValues()[a.getIndex()]));

            double policySum = policy.values().stream()
                    .mapToDouble(Double::valueOf)
                    .sum();
            for (Map.Entry<Action, Float> e : policy.entrySet()) {
                Action action = e.getKey();
                Float p = e.getValue();
                node.getChildren().put(action, new Node(config, p / policySum));  // p/policySum = probability that this action is chosen
            }

        }
    }

    public Map.@NotNull Entry<Action, Node> selectChild(@NotNull Node node, MinMaxStats minMaxStats) {
        Action action = selectAction(node, minMaxStats);
        return Map.entry(action, node.getChildren().get(action));
    }

    public Action selectAction(@NotNull Node node, MinMaxStats minMaxStats) {
        List<Pair<Action, Double>> distributionInput = regularizedPolicyOptimization.getDistributionInput(node, minMaxStats);
        return selectActionByDrawingFromDistribution(distributionInput);
    }

    public Action selectActionByMax(@NotNull Node node, MinMaxStats minMaxStats) {
        List<Pair<Action, Double>> distributionInput = regularizedPolicyOptimization.getDistributionInput(node, minMaxStats);
        return distributionInput.stream().max(Comparator.comparing(Pair::getValue)).orElseThrow(MuZeroException::new).getKey();
    }

    public Action selectActionByDrawingFromDistribution(List<Pair<Action, Double>> distributionInput) {
        EnumeratedDistribution<Action> distribution;
        try {
            distribution = new EnumeratedDistribution<>(rng, distributionInput);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }
        return distribution.sample();
    }


    public Action selectActionByMaxFromDistribution(List<Pair<Action, Double>> distributionInput) {
        Collections.shuffle(distributionInput);
        return distributionInput.stream()
                .max(Comparator.comparing(Pair::getSecond))
                .orElseThrow(MuZeroException::new).getKey();
    }
}
