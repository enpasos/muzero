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

package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.memory.protobuf.GameProto;
import ai.enpasos.muzero.platform.agent.memory.protobuf.LegalActionProtos;
import ai.enpasos.muzero.platform.agent.memory.protobuf.ObservationProtos;
import ai.enpasos.muzero.platform.agent.memory.protobuf.PolicyProtos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public class GameDTO implements Comparable<GameDTO> {

    String networkName = "NONE";
    float pRandomActionRawSum;
    int pRandomActionRawCount;
    @EqualsAndHashCode.Include
    private List<Integer> actions;
    private List<Float> rewards;

    private List<Float> entropies;
    private List<float[]> policyTargets;

    private int observationPartSize;
    private List<Observation> observations;



    private List<float[]> playoutPolicy;

    private List<boolean[]> legalActions;  // obsolete
    private List<Float> rootValueTargets;
    private List<Float> rootEntropyValueTargets;
    private List<Float> rootEntropyValuesFromInitialInference;
    private List<Float> rootValuesFromInitialInference;
    private float lastValueError;
    private long count;
    private long nextSurpriseCheck;
    private boolean surprised;


    private boolean hybrid;
    private long tSurprise;
    private long tHybrid = -1;
    private long tStateA;  // obsolete
    private long tStateB;  // obsolete


    private int trainingEpoch;


    private int tdSteps;
    private List<Float> maxEntropies;  // obsolete ??

    public GameDTO(List<Integer> actions) {
        this();
        this.actions.addAll(actions);
    }

    public GameDTO() {
        this.actions = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.policyTargets = new ArrayList<>();
        this.observations = new ArrayList<>();
        this.playoutPolicy = new ArrayList<>();
        this.legalActions = new ArrayList<>();
        this.rootValueTargets = new ArrayList<>();
        this.rootEntropyValueTargets = new ArrayList<>();
        this.entropies = new ArrayList<>();
        this.maxEntropies = new ArrayList<>();
        this.rootValuesFromInitialInference = new ArrayList<>();
        this.rootEntropyValuesFromInitialInference = new ArrayList<>();
        this.surprised = false;
        this.hybrid = false;
    }

    public boolean hasExploration() {
        return tHybrid > 0;
    }

    public double getAverageEntropy() {
        return IntStream.range(0, entropies.size())
            .mapToDouble(i -> entropies.get(i))
            .sum() / Math.max(1, entropies.size());
    }

    public double getAverageMaxEntropy() {
        return IntStream.range(0, maxEntropies.size())
            .mapToDouble(i -> maxEntropies.get(i))
            .sum() / Math.max(1, maxEntropies.size());
    }


    public GameDTO copy(int toPosition) {
        GameDTO copy = new GameDTO();

        copy.networkName = this.networkName;
        copy.surprised = this.surprised;
        copy.hybrid = this.hybrid;
        copy.tSurprise = this.tSurprise;
        copy.tHybrid = this.tHybrid;
        copy.tStateA = this.tStateA;
        copy.tStateB = this.tStateB;
        copy.tdSteps = this.tdSteps;
        copy.trainingEpoch = this.trainingEpoch;
        copy.observationPartSize = this.observationPartSize;
        copy.nextSurpriseCheck = this.nextSurpriseCheck;
        copy.pRandomActionRawSum = this.pRandomActionRawSum;
        copy.pRandomActionRawCount = this.pRandomActionRawCount;
        if (toPosition > 0) {
            copy.rewards.addAll(this.rewards.subList(0, toPosition));
            copy.entropies.addAll(this.entropies.subList(0, toPosition));
            copy.maxEntropies.addAll(this.maxEntropies.subList(0, toPosition));
            copy.actions.addAll(this.actions.subList(0, toPosition));

            this.policyTargets.subList(0, toPosition).forEach(pT -> copy.policyTargets.add(Arrays.copyOf(pT, pT.length)));
            this.observations.subList(0, toPosition).forEach(pT -> copy.observations.add(pT.clone()));
            this.playoutPolicy.subList(0, toPosition).forEach(pT -> copy.playoutPolicy.add(Arrays.copyOf(pT, pT.length)));
            this.legalActions.subList(0, toPosition).forEach(pT -> copy.legalActions.add(Arrays.copyOf(pT, pT.length)));
            if (this.rootValueTargets.size() >= toPosition)
                copy.rootValueTargets.addAll(this.rootValueTargets.subList(0, toPosition));

            if (this.rootEntropyValueTargets.size() >= toPosition)
                copy.rootEntropyValueTargets.addAll(this.rootEntropyValueTargets.subList(0, toPosition));
            if (this.rootValuesFromInitialInference.size() >= toPosition)
                copy.rootValuesFromInitialInference.addAll(this.rootValuesFromInitialInference.subList(0, toPosition));
            if (this.rootEntropyValuesFromInitialInference.size() >= toPosition)
                copy.rootEntropyValuesFromInitialInference.addAll(this.rootEntropyValuesFromInitialInference.subList(0, toPosition));
        }
        return copy;
    }

    public GameDTO copyWithoutActions() {
        GameDTO copy = new GameDTO();
        copy.networkName = this.networkName;
        copy.count = this.count;
        copy.nextSurpriseCheck = this.nextSurpriseCheck;
        return copy;
    }

    public GameDTO copy() {
        GameDTO copy = new GameDTO();
        copy.networkName = this.networkName;
        copy.count = this.count;
        copy.nextSurpriseCheck = this.nextSurpriseCheck;
        copy.rewards.addAll(this.rewards);
        copy.surprised = this.surprised;
        copy.hybrid = this.hybrid;
        copy.tSurprise = this.tSurprise;
        copy.tHybrid = this.tHybrid;
        copy.tStateA = this.tStateA;
        copy.tStateB = this.tStateB;
        copy.tdSteps = this.tdSteps;
        copy.trainingEpoch = this.trainingEpoch;
        copy.observationPartSize = this.observationPartSize;
        copy.entropies.addAll(this.entropies);
        copy.maxEntropies.addAll(this.maxEntropies);
        copy.actions.addAll(this.actions);

        copy.pRandomActionRawSum = this.pRandomActionRawSum;
        copy.pRandomActionRawCount = this.pRandomActionRawCount;
        this.policyTargets.forEach(pT -> copy.policyTargets.add(Arrays.copyOf(pT, pT.length)));
        this.observations.forEach(pT -> copy.observations.add(pT.clone()));
        this.playoutPolicy.forEach(pT -> copy.playoutPolicy.add(Arrays.copyOf(pT, pT.length)));
        this.legalActions.forEach(pT -> copy.legalActions.add(Arrays.copyOf(pT, pT.length)));
        copy.rootValueTargets.addAll(this.rootValueTargets);
        copy.rootEntropyValueTargets.addAll(this.rootEntropyValueTargets);
        copy.rootEntropyValuesFromInitialInference.addAll(this.rootEntropyValuesFromInitialInference);
        return copy;
    }

    public GameProto proto() {
        GameProto.Builder gameBuilder = GameProto.newBuilder();
        gameBuilder.setNetworkName(this.networkName);
        gameBuilder.setLastValueError(this.lastValueError);
        gameBuilder.setCount(this.count);
        gameBuilder.setNextSurpriseCheck(this.nextSurpriseCheck);
        gameBuilder.setSurprised(this.surprised);
        gameBuilder.setHybrid(this.hybrid);
        gameBuilder.setTSurprise(this.tSurprise);
        gameBuilder.setTHybrid(this.tHybrid);
        gameBuilder.setTStateA(this.tStateA);
        gameBuilder.setTStateB(this.tStateB);
        gameBuilder.setTdSteps(this.tdSteps);
        gameBuilder.setTrainingEpoch(this.trainingEpoch);
        gameBuilder.setObservationPartSize(this.observationPartSize);
        gameBuilder.addAllActions(getActions());
        gameBuilder.setPRandomActionRawSum(this.pRandomActionRawSum);
        gameBuilder.setPRandomActionRawCount(this.pRandomActionRawCount);
        gameBuilder.addAllRewards(getRewards());
        gameBuilder.addAllRootValueTargets(getRootValueTargets());
        gameBuilder.addAllRootEntropyValueTargets(getRootEntropyValueTargets());
        gameBuilder.addAllEntropies(getEntropies());
        gameBuilder.addAllMaxEntropies(getMaxEntropies());
        gameBuilder.addAllRootValuesFromInitialInference(getRootValuesFromInitialInference());
        gameBuilder.addAllRootEntropyValuesFromInitialInference(getRootEntropyValuesFromInitialInference());
        getPlayoutPolicy().forEach(policy  -> {
            PolicyProtos.Builder b = PolicyProtos.newBuilder();
            IntStream.range(0, policy.length).forEach(i ->
                b.addPolicy(policy[i])
            );
            gameBuilder.addPlayoutPolicy(b.build());
        });
        getPolicyTargets().forEach(policyTarget -> {
            PolicyProtos.Builder b = PolicyProtos.newBuilder();
            IntStream.range(0, policyTarget.length).forEach(i ->
                b.addPolicy(policyTarget[i])
            );
            gameBuilder.addPolicyTargets(b.build());
        });
        getObservations().forEach(observation -> {
            ObservationProtos.Builder b = ObservationProtos.newBuilder();
            b.setObservation(observation.toByteString());
            gameBuilder.addObservations(b.build());
        });
        getLegalActions().forEach(legalActionsLocal -> {
            LegalActionProtos.Builder b = LegalActionProtos.newBuilder();
            IntStream.range(0, legalActionsLocal.length).forEach(i ->
                b.addLegalAction(legalActionsLocal[i])
            );
            gameBuilder.addLegalActions(b.build());
        });
        return gameBuilder.build();
    }

    public void deproto(GameProto p) {
        this.setNetworkName(p.getNetworkName());
        this.setSurprised(p.getSurprised());
        this.setHybrid(p.getHybrid());
        this.setTSurprise(p.getTSurprise());
        this.setTHybrid(p.getTHybrid());
        this.setTStateA(p.getTStateA());
        this.setTStateB(p.getTStateB());
        this.setTdSteps(p.getTdSteps());
        this.setTrainingEpoch(p.getTrainingEpoch());
        this.setObservationPartSize(p.getObservationPartSize());
        this.setActions(p.getActionsList());
        this.setPRandomActionRawSum(p.getPRandomActionRawSum());
        this.setPRandomActionRawCount(p.getPRandomActionRawCount());
        this.setRewards(p.getRewardsList());
        this.setRootValueTargets(p.getRootValueTargetsList());
        this.setRootEntropyValueTargets(p.getRootEntropyValueTargetsList());
        this.setEntropies(p.getEntropiesList());
        this.setMaxEntropies(p.getMaxEntropiesList());
        this.setLastValueError(p.getLastValueError());
        this.setRootValuesFromInitialInference(p.getRootValuesFromInitialInferenceList());
        this.setRootEntropyValuesFromInitialInference(p.getRootEntropyValuesFromInitialInferenceList());
        this.setCount(p.getCount());
        this.setNextSurpriseCheck(p.getNextSurpriseCheck());
        if (p.getPolicyTargetsCount() > 0) {
            this.setPolicyTargets(p.getPolicyTargetsList().stream().map(policy -> {
                        float[] result = new float[p.getPolicyTargets(0).getPolicyCount()];
                        int i = 0;
                        for (Float f : policy.getPolicyList()) {
                            result[i++] = f;
                        }
                        return result;
                    }
                )
                .collect(Collectors.toList()));
        }
        if (p.getObservationsCount() > 0) {
            this.setObservations(
                    p.getObservationsList().stream()
                            .map(o -> ObservationTwoPlayers.fromByteStringAndPartSize(o.getObservation(), this.observationPartSize))
                            .collect(Collectors.toList())
            );
        }
        if (p.getPlayoutPolicyCount() > 0) {
            this.setPlayoutPolicy(p.getPlayoutPolicyList().stream().map(policyProtos -> {
                        float[] result = new float[p.getPlayoutPolicy(0).getPolicyCount()];
                        int i = 0;
                        for (Float f : policyProtos.getPolicyList()) {
                            result[i++] = f;
                        }
                        return result;
                    }
                )
                .collect(Collectors.toList()));
        }
        if (p.getLegalActionsCount() > 0) {
            this.setLegalActions(p.getLegalActionsList().stream().map(legalActionProtos -> {
                        boolean[] result = new boolean[p.getLegalActions(0).getLegalActionCount()];
                        int i = 0;
                        for (Boolean b : legalActionProtos.getLegalActionList()) {
                            result[i++] = b;
                        }
                        return result;
                    }
                )
                .collect(Collectors.toList()));
        }

    }


    @Override
    public int compareTo(@NotNull GameDTO o) {
        return Long.compare(this.getCount(), o.getCount());
    }
}

