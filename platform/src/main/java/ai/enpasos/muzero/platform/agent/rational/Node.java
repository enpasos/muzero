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
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.add;
import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.sigmas;
import static ai.enpasos.muzero.platform.common.Functions.softmax;

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
    private double improvedValue;
    private double valueFromInitialInference;
    private NDArray hiddenState;
    private double reward;
    private double multiplierLambda;
    private double valueSum;
    private double vmix;
    private Action action;
    private int visitCount;
    @Builder.Default
    private boolean root = false;
    private MuZeroConfig config;
    private GumbelAction gumbelAction;
    private double improvedPolicyValue;
    private double improvedPolicyValue2;
    private double improvedPolicyValue3;

    public Node(MuZeroConfig config, double prior, boolean root) {
        this(config, prior);
        this.root = root;
        this.valueFromInitialInference = 100000f; // to produce a high error if not changed

    }

    public Node(MuZeroConfig config, double prior) {
        this.config = config;
        this.visitCount = 0;
        this.prior = prior;
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

    public void calculateVmix() {

        double vHat = this.getValueFromNetwork();
        vmix = vHat;
        if (this.getVisitCount() == 0) return;

        double b = this.getChildren().stream().filter(node -> node.getVisitCount() > 0)
            .mapToDouble(node -> node.getPrior() * node.getQValue()).sum();
        double c = this.getChildren().stream().filter(node -> node.getVisitCount() > 0)
            .mapToDouble(Node::getPrior).sum();
        int d = this.getChildren().stream()
            .mapToInt(Node::getVisitCount).sum();

        if (d == 0d) {
            vmix = vHat;
        } else {
            vmix = 1d / (1d + d) * (vHat + d / c * b);  // check signs
        }
    }

    public double[] getCompletedQValuesNormalized(MinMaxStats minMaxStats) {
        return children.stream().mapToDouble(node -> {
                Node child = node;
                if (child.getVisitCount() > 0) {
                    return child.getQValue();
                } else {
                    return getVmix();
                }
            })
            .map(minMaxStats::normalize)
            .toArray();
    }

    public void calculateImprovedPolicy(MinMaxStats minMaxStats) {
        int maxActionVisitCount = getChildren().stream().mapToInt(Node::getVisitCount).max().getAsInt();
        double temperature = this.isRoot() ? config.getTemperatureRoot() : config.getTemperatureNonRoot();
        double[] logits = getChildren().stream().mapToDouble(node -> node.getLogit() / temperature).toArray();
        double[] completedQsNormalized = getCompletedQValuesNormalized(minMaxStats);
        double[] raw = add(logits, sigmas(completedQsNormalized, maxActionVisitCount, config.getCVisit(), config.getCScale()));
        double[] improvedPolicy = softmax(raw);
        IntStream.range(0, improvedPolicy.length).forEach(i -> getChildren().get(i).improvedPolicyValue = improvedPolicy[i]);
    }


    public Action getRandomAction() {
        int index = ThreadLocalRandom.current().nextInt(0, children.size() - 1);
        return children.get(index).getAction();
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


    public double getQValue() {
        if (visitCount == 0) {
            if (this.isRoot()) return 0.0;
            // then complete
            return this.parent.getVmix();
        }
        return this.getValueSum() / this.visitCount;
    }


    public void expand(Player toPlay, NetworkIO networkOutput) {
        if (networkOutput != null)
            setValueFromNetwork(networkOutput.getValue());

        setToPlay(toPlay);

        if (networkOutput == null)
            throw new MuZeroException("networkOutput must not be null");
        setValueFromInitialInference(networkOutput.getValue());
        setHiddenState(networkOutput.getHiddenState());
        setReward(networkOutput.getReward());

        Map<Action, Pair<Float, Float>> policyMap = new HashMap<>();
        for (int i = 0; i < networkOutput.getPolicyValues().length; i++) {
            policyMap.put(config.newAction(i), new Pair<>(
                    networkOutput.getPolicyValues()[i],
                    networkOutput.getLogits()[i]
                )
            );
        }

        double policySum = policyMap.values().stream()
            .mapToDouble(p -> p.getFirst().doubleValue())
            .sum();
        for (Map.Entry<Action, Pair<Float, Float>> e : policyMap.entrySet()) {
            Action action2 = e.getKey();
            Float p = e.getValue().getFirst();
            Float logit2 = e.getValue().getSecond();
            getChildren().add(Node.builder().parent(this).action(action2).config(config).prior(p / policySum).logit(logit2).build());
        }


    }


    public void expandRootNode(Player toPlay, @NotNull List<Action> actions, NetworkIO networkOutput,
                               boolean fastRuleLearning) {
        if (!this.root) throw new MuZeroException("expandRootNode should only be called on the root node");
        if (networkOutput != null)
            setValueFromNetwork(networkOutput.getValue());

        setToPlay(toPlay);
        if (!fastRuleLearning) {
            if (networkOutput == null) {
                throw new MuZeroException("networkOutput must not be null here");
            }
            setValueFromInitialInference(networkOutput.getValue());
            setHiddenState(networkOutput.getHiddenState());
            setReward(networkOutput.getReward());
        }
        if (fastRuleLearning) {
            double p = 1d / actions.size();
            for (Action action2 : actions) {
                getChildren().add(Node.builder().parent(this).action(action2).config(config).prior(p).build());
            }
        } else {
            Map<Action, Pair<Float, Float>> policy = actions.stream()
                .collect(Collectors.toMap(a -> a, a ->
                        new Pair<>(
                            networkOutput.getPolicyValues()[a.getIndex()],
                            networkOutput.getLogits()[a.getIndex()])
                    )

                );

            double policySum = policy.values().stream()
                .mapToDouble(p -> p.getFirst().doubleValue())
                .sum();
            for (Map.Entry<Action, Pair<Float, Float>> e : policy.entrySet()) {
                Action action2 = e.getKey();
                Float p = e.getValue().getFirst();
                Float logit2 = e.getValue().getSecond();
                getChildren().add(Node.builder().parent(this).action(action2).config(config).prior(p / policySum).logit(logit2).build());
            }

        }
    }

    public double comparisonValue(int nSum) {
        return improvedPolicyValue - visitCount / (1.0 + nSum);
    }


    public Node selectChild(MinMaxStats minMaxStats) {

        calculateImprovedPolicy(minMaxStats);
        int nSum = this.getChildren().stream().mapToInt(Node::getVisitCount).sum();
        this.getChildren().forEach(n -> n.improvedPolicyValue2 = n.comparisonValue(nSum));
        return this.getChildren().stream().max(Comparator.comparing(Node::getImprovedPolicyValue2)).get();

    }

    public void calculateImprovedValue() {
        this.improvedValue = this.getChildren().stream()
            .mapToDouble(node -> node.getImprovedPolicyValue() * node.getImprovedValue())
            .sum();
    }


}
