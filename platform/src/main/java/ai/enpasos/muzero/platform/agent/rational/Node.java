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

package ai.enpasos.muzero.platform.agent.rational;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.rational.gumbel.GumbelAction;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.rational.EpisodeManager.softmax;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.Gumbel.add;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.SequentialHalving.sigmas;

@Data
@Builder
@AllArgsConstructor
public class Node {
    @Builder.Default
    List<Node> children = new ArrayList<>();
    List<Node> searchPath;
    private Node parent;
    private Player toPlay;
    private double logit;
    private double pseudoLogit;
    private double prior;
    private double valueFromNetwork;
    private double valueFromInitialInference;
    private NDArray hiddenState;
    private double reward;
    private double multiplierLambda;
    private double valueSum;
    private Action action;
    private int visitCount;
    @Builder.Default
    private boolean root = false;
    private MuZeroConfig config;
    private GumbelAction gumbelAction;
    private double improvedPolicyValue;
    private double improvedPolicyValue2;

    public Node(MuZeroConfig config, double prior, boolean root) {
        this(config, prior);
        this.root = root;
        this.valueFromInitialInference = 100000f; // to produce a high error if not changed

    }

    public Node(MuZeroConfig config, double prior) {
        this.config = config;
        this.visitCount = 0;
        this.prior = prior;
        this.valueSum = 0;
        this.children = new ArrayList<>();
        hiddenState = null;
        reward = 0.0;
        this.valueFromInitialInference = 100000f;  // to produce a high error if not changed
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
        if (this.getGumbelAction() != null) {
            this.getGumbelAction().setVisitCount(visitCount);
        }
    }

    public double getVmix() {

        // this is in the perspective of the player to play
        double vHat = this.getValueFromNetwork();

        if (this.getVisitCount() == 0) return vHat;

        double b = this.getChildren().stream().filter(node -> node.getVisitCount() > 0)
            .mapToDouble(node -> node.getPrior() * node.qValue()).sum();
        double c = this.getChildren().stream().filter(node -> node.getVisitCount() > 0)
            .mapToDouble(node -> node.getPrior()).sum();
        int d = this.getChildren().stream()
            .mapToInt(node -> node.getVisitCount()).sum();

        if (d == 0d) return vHat; // no visits on the children
        double vmix = 1d / (1d + d) * (vHat + d / c * b);  // check signs
        return vmix;

    }

    public double[] getCompletedQValues(MinMaxStats minMaxStats) {

        double vMix = getVmix();

        double vMixFinal = vMix;
        return IntStream.range(0, children.size()).mapToDouble(i -> {
                Node child = children.get(i);
                if (child.getVisitCount() > 0) {
                    return child.qValue();
                } else {
                    return vMixFinal;
                }
            })
            .map(v -> minMaxStats.normalize(v))
            .toArray();

    }

    public void updateImprovedPolicyValueOnChildren(MinMaxStats minMaxStats) {
        int maxActionVisitCount = getChildren().stream().mapToInt(a -> a.getVisitCount()).max().getAsInt();
        double[] logits = getChildren().stream().mapToDouble(node -> {
            return node.getLogit();
        }).toArray();


        double[] completedQs = getCompletedQValues(minMaxStats);

        double[] raw = add(logits, sigmas(completedQs, maxActionVisitCount, config.getCVisit(), config.getCScale()));

        double[] improvedPolicy = softmax(raw);

        IntStream.range(0, improvedPolicy.length).forEach(i -> getChildren().get(i).improvedPolicyValue = improvedPolicy[i]);

    }

    public void initGumbelAction(int actionIndex, double policyValue) {
        gumbelAction = GumbelAction.builder()
            .policyValue(policyValue)
            .logit(this.logit)
            .actionIndex(actionIndex)
            .build();
        gumbelAction.initGumbelValue();
        gumbelAction.setNode(this);
    }

    public boolean expanded() {
        return this.getChildren().size() > 0;
    }


    public double qValue() {
        double value = value();
        if (config.getPlayerMode() == PlayerMode.TWO_PLAYERS) {
            return -value;
        } else {
            return value;
        }
    }

    public double value() {
        if (visitCount == 0) return 0.0;
        // is from perspective of the player to play at the root node for this node (not the parent)
        return this.getValueSum() / this.visitCount;
    }


    public void expand(Player toPlay, NetworkIO networkOutput) {
        if (networkOutput != null)
            setValueFromNetwork(networkOutput.getValue());

        setToPlay(toPlay);

        setValueFromInitialInference(networkOutput.getValue());
        setHiddenState(networkOutput.getHiddenState());
        setReward(networkOutput.getReward());

        Map<Action, Pair<Float, Float>> policyMap = new HashMap<>();
        for (int i = 0; i < networkOutput.getPolicyValues().length; i++) {
            policyMap.put(config.newAction(i), new Pair(
                    networkOutput.getPolicyValues()[i],
                    networkOutput.getLogits()[i]
                )
            );
        }
//        for (Node a : children) {
//            policyMap.put(a.getAction(), new Pair(
//                    networkOutput.getPolicyValues()[a.getAction().getIndex()],
//                    networkOutput.getLogits()[a.getAction().getIndex()]
//                )
//            );
//        }

        double policySum = policyMap.values().stream()
            .mapToDouble(p -> p.getFirst().doubleValue())
            .sum();
        for (Map.Entry<Action, Pair<Float, Float>> e : policyMap.entrySet()) {
            Action action = e.getKey();
            Float p = e.getValue().getFirst();
            Float logit = e.getValue().getSecond();
            getChildren().add(Node.builder().parent(this).action(action).config(config).prior(p / policySum).logit(logit).build());
        }


    }


    public void expandRootNode(Player toPlay, @NotNull List<Action> actions, NetworkIO networkOutput,
                               boolean fastRuleLearning) {
        if (!this.root) throw new MuZeroException("expandRootNode should only be called on the root node");
        if (networkOutput != null)
            setValueFromNetwork(networkOutput.getValue());

        setToPlay(toPlay);
        if (!fastRuleLearning) {
            setValueFromInitialInference(networkOutput.getValue());
            setHiddenState(networkOutput.getHiddenState());
            setReward(networkOutput.getReward());
        }
        if (fastRuleLearning) {
            double p = 1d / actions.size();
            for (Action action : actions) {
                getChildren().add(Node.builder().parent(this).action(action).config(config).prior(p).build());
            }
        } else {
            Map<Action, Pair<Float, Float>> policy = actions.stream()
                .collect(Collectors.toMap(a -> a, a ->
                        new Pair(
                            networkOutput.getPolicyValues()[a.getIndex()],
                            networkOutput.getLogits()[a.getIndex()])
                    )

                );

            double policySum = policy.values().stream()
                .mapToDouble(p -> p.getFirst().doubleValue())
                .sum();
            for (Map.Entry<Action, Pair<Float, Float>> e : policy.entrySet()) {
                Action action = e.getKey();
                Float p = e.getValue().getFirst();
                Float logit = e.getValue().getSecond();
                getChildren().add(Node.builder().parent(this).action(action).config(config).prior(p / policySum).logit(logit).build());
            }

        }
    }

    public double comparisonValue(int nSum) {
        return improvedPolicyValue - visitCount / (1.0 + nSum);
    }


    public Node selectChild(MinMaxStats minMaxStats) {
        updateImprovedPolicyValueOnChildren(minMaxStats);
        int nSum = this.getChildren().stream().mapToInt(node -> node.getVisitCount()).sum();
        this.getChildren().stream().forEach(n -> n.improvedPolicyValue2 = n.comparisonValue(nSum));
        return this.getChildren().stream().max(Comparator.comparing(Node::getImprovedPolicyValue2)).get();
    }
}
