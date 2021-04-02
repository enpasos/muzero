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

package ai.enpasos.muzero.play;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.network.Network;
import ai.enpasos.muzero.network.NetworkIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.environments.EnvironmentBaseBoardGames.render;

@Slf4j
public class MCTS {
    private final MuZeroConfig config;

    private final RandomGenerator rng;

    public MCTS(MuZeroConfig config) {
        this.config = config;
        this.rng = new Well19937c();
    }


    public static void backpropagate(List<Node> searchPath, double value, Player toPlay, double discount, MinMaxStats minMaxStats) {
        for (int i = searchPath.size() - 1; i >= 0; i--) {
            Node node = searchPath.get(i);
            if (node.getToPlay() == toPlay) {
                node.setValueSum(node.getValueSum() + value);
            } else {
                node.setValueSum(node.getValueSum() - value);
            }
            node.setVisitCount(node.getVisitCount() + 1);
            minMaxStats.update(node.value());
            value = node.reward + discount * value;
        }
    }


    private void printUCBScores(MuZeroConfig config, List<Node> searchPath, MinMaxStats minMaxStats, boolean ucbScoreExplained) {
        for (int i = 0; i < searchPath.size(); i++) {
            Node node = searchPath.get(i);
            printUCBScores(config, node, minMaxStats, ucbScoreExplained);
        }
    }

    private void printUCBScores(MuZeroConfig config, Node node, MinMaxStats minMaxStats, boolean ucbScoreExplained) {
        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
        for (int i = 0; i < config.getActionSpaceSize(); i++) {
            Action action = new Action(config, i);
            Node child = node.getChildren().get(action);
            double score = 0d;
            if (child != null) {
                score = ucbScore(node, child, minMaxStats, ucbScoreExplained);

                values[Action.getRow(config, i)][Action.getCol(config, i)] =
                        String.format("%3d", Math.round(100.0 * score));
            }
        }

        System.out.println(render(config, values));
    }

//    private static void renderNetworkGuess(MuZeroConfig config, Player toPlay, NetworkIO networkOutput, boolean gameOver) {
//        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
//        if (networkOutput != null) {
//            double v = networkOutput.getValue();
//            double p = (v + 1) / 2 * 100;
//            int percent = (int) Math.round(p);
//            if (!gameOver) {
//                for (int i = 0; i < networkOutput.getPlainPolicyLogits().length; i++) {
//                    values[Action.getRow(config, i)][Action.getCol(config, i)] = String.format("%2d", Math.round(100.0 * networkOutput.getPlainPolicyLogits()[i])) + "%";  // because softmax
//                }
//                System.out.println(render(config, values));
//
//            }
//            System.out.println("Estimated chance for " + ((OneOfTwoPlayer) toPlay).getSymbol() + " to win: " + percent + "%");
//
//        }
//    }

    public void run(Node root, ActionHistory actionHistory, Network network,
                    Duration inferenceDuration, List<NDArray> actionSpaceOnDevice) {


        runParallel(List.of(root), List.of(actionHistory), network,
                inferenceDuration, actionSpaceOnDevice, 0);
    }

    public void runParallel(List<Node> rootList, List<ActionHistory> actionHistoryList, Network network,
                            Duration inferenceDuration, List<NDArray> actionSpaceOnDevice, int indexOfJustOneOfTheGames) {


        // TODO better would be a simulation object that encapsulates the info that is spread over many Lists
        List<MinMaxStats> minMaxStatsList = IntStream.rangeClosed(1, rootList.size())
                .mapToObj(i -> new MinMaxStats(config.getKnownBounds()))
                .collect(Collectors.toList());


        for (int i = 0; i < config.getNumSimulations(); i++) { // in range(config.num_simulations):

            List<ActionHistory> historyList = actionHistoryList.stream().map(a -> a.clone()).collect(Collectors.toList());

            List<Node> nodeList = new ArrayList<>();
            nodeList.addAll(rootList);


            List<List<Node>> searchPathList = IntStream.range(0, rootList.size())
                    .mapToObj(nL -> new ArrayList<Node>())
                    .collect(Collectors.toList());
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


            List<NDArray> hiddenStateList = searchPathList.stream().map(searchPath -> {
                Node parent = searchPath.get(searchPath.size() - 2);
                return parent.hiddenState;
            }).collect(Collectors.toList());


            List<Action> lastActions = historyList.stream().map(ah -> ah.lastAction()).collect(Collectors.toList());
            List<NDArray> actionList = lastActions.stream().map(action -> {
                NDArray a = actionSpaceOnDevice.get(action.getIndex());
                return a;
                //   return a.duplicate();
            }).collect(Collectors.toList());

//                // Inside the search tree we use the dynamics function to obtain the next
//                // hidden state given an action and the previous hidden state.
//                Node parent = searchPath.get(searchPath.size() - 2);
//
//
            if (inferenceDuration != null) inferenceDuration.value -= System.currentTimeMillis();
            List<NetworkIO> networkOutputList = network.recurrentInferenceList(hiddenStateList,
                    actionList
            );
            if (inferenceDuration != null) inferenceDuration.value += System.currentTimeMillis();
//
            for (int g = 0; g < nodeList.size(); g++) {
                ActionHistory history = historyList.get(g);
                Node node = nodeList.get(g);
                NetworkIO networkOutput = networkOutputList.get(g);
                List<Node> searchPath = searchPathList.get(g);
                MinMaxStats minMaxStats = minMaxStatsList.get(g);

                expandNode(node, history.toPlay(), history.actionSpace(config), networkOutput, false);

                backpropagate(searchPath, networkOutput.getValue(), history.toPlay(), config.getDiscount(), minMaxStats);

                if (g == indexOfJustOneOfTheGames) {
                    // System.out.println(i + ": " + searchPathToString(searchPath, false));
                }

            }


        }


        clean(rootList);
    }

    private void clean(List<Node> rootList) {
        rootList.stream().forEach(
                root -> clean(root)
        );
    }

    private void clean(Node node) {
        if (node.getHiddenState() != null) {
            node.getHiddenState().close();
            node.setHiddenState(null);
        }
        node.getChildren().entrySet().stream().forEach(
                e -> {
                    Node child = e.getValue();
                    clean(child);
                }
        );
    }

    private String searchPathToString(List<Node> searchPath, boolean withValue) {
        StringBuffer buf = new StringBuffer();
        searchPath.stream().forEach(
                n -> {
                    if (n.getAction() != null) {
                        buf.append(", ")
                                .append(n.getAction().getIndex());
                        if (withValue) {
                            buf.append("(")
                                    .append(n.value())
                                    .append(")");
                        }
                    } else {
                        buf.append("root");
                    }
                }
        );
        return buf.toString();
    }


    public void expandNode(Node node, Player toPlay, List<Action> actions, NetworkIO networkOutput,
                           boolean fastRuleLearning) {
        node.toPlay = toPlay;
        if (!fastRuleLearning) {
            node.hiddenState = networkOutput.getHiddenState();
            node.reward = networkOutput.getReward();
        }
        if (fastRuleLearning) {
            double p = 1d / actions.size();
            for (Action action : actions) {
                node.children.put(action, new Node(p));  // p/policySum = probability that this action is chosen
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
                node.children.put(action, new Node(p / policySum));  // p/policySum = probability that this action is chosen
            }
        }
    }
//    public void expandNodeParallel (List<Node> nodeList, List<Player> toPlayList, List<List<Action>> actionsList, NetworkIO networkOutput,
//                            boolean fastRuleLearning){
//
//        for (int i = 0; i < nodeList.size(); i++) {
//            nodeList.get(i).toPlay = toPlayList
//        }
//        node.toPlay = toPlay;
//        if (!fastRuleLearning) {
//            node.hiddenState = networkOutput.getHiddenState();
//            node.reward = networkOutput.getReward();
//        }
//
//
//
//        if (fastRuleLearning) {
//            double p = 1d / actions.size();
//            for (Action action : actions) {
//                node.children.put(action, new Node(p));  // p/policySum = probability that this action is chosen
//            }
//        } else {
//            Map<Action, Float> policy = actions.stream()
//                    .collect(Collectors.toMap(a -> a, a -> networkOutput.getPolicyValues()[a.getIndex()]));
//
//
//            //
//            //
//
//            double policySum = policy.values().stream()
//                    .mapToDouble(Double::valueOf)
//                    .sum();
//            for (Map.Entry<Action, Float> e : policy.entrySet()) {
//                Action action = e.getKey();
//                Float p = e.getValue();
//                node.children.put(action, new Node(p / policySum));  // p/policySum = probability that this action is chosen
//            }
//        }
//    }


    public Map.Entry<Action, Node> selectChild(Node node, MinMaxStats minMaxStats) {

        List<Map.Entry<Action, Node>> list = new ArrayList<>();
        list.addAll(node.children.entrySet());
        Collections.shuffle(list);
        Map.Entry<Action, Node> result = list.stream()
                .max(Comparator.comparing(e -> ucbScore(node, e.getValue(), minMaxStats, false)))
                .get();


        // for debugging only
        // ucbScore(node, result.getValue(), minMaxStats, true);

        return result;

    }


    public double ucbScore(Node parent, Node child, MinMaxStats minMaxStats, boolean specialLogging) {
        double pbC = Math.log((parent.getVisitCount() + config.getPbCBase() + 1d) / config.getPbCBase()) + config.getPbCInit();
        pbC *= Math.sqrt(parent.getVisitCount()) / (child.getVisitCount() + 1);
        double priorScore = pbC * child.prior;
        double valueScore = 0d;
        if (child.getVisitCount() > 0) {
            valueScore = minMaxStats.normalize(child.getReward() + config.getDiscount() * child.value());
        }
        double score = priorScore + valueScore;

        if (specialLogging) {
            System.out.println("ucbScore = " + pbC + " * " + child.prior + " + " + valueScore + " = " + score);
        }
        return score;
    }


    public Action selectActionByDrawingFromDistribution(int numMoves, Node node, Network network) {

        List<Pair<Action, Double>> distributionInput = getActionDistributionInput(numMoves, node, network);

        EnumeratedDistribution<Action> distribution = new EnumeratedDistribution<>(rng, distributionInput);
        return distribution.sample();
    }

    public Action selectActionByMaxFromDistribution(int numMoves, Node node, Network network) {

        List<Pair<Action, Double>> distributionInput = getActionDistributionInput(numMoves, node, network);
        return distributionInput.stream().max(Comparator.comparing(Pair::getValue)).get().getKey();

    }

    private List<Pair<Action, Double>> getActionDistributionInput(int numMoves, Node node, Network network) {
        int numTrainingSteps = network != null ? network.trainingSteps() : 1;
        double t = config.getVisitSoftmaxTemperatureFn().apply(numMoves, numTrainingSteps);
        double td = 1.0 / t;

        Double sum = node.getChildren().entrySet().stream()
                .collect(Collectors.summingDouble(e -> Math.pow(e.getValue().getVisitCount(), td)));

        List<Pair<Action, Double>> distributionInput = node.getChildren().entrySet().stream()
                .map(e -> {
                    Action action = e.getKey();
                    double v = Math.pow(e.getValue().getVisitCount(), td) / sum;
                    Pair<Action, Double> pair = new Pair<>(action, v);
                    return pair;
                })
                .collect(Collectors.toList());
        return distributionInput;
    }

    private double visitSoftmaxTemperature(int numMoves, int numTrainingSteps) {
        return config.getVisitSoftmaxTemperatureFn().apply(numMoves, numTrainingSteps);
    }

}
