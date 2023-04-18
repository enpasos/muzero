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
    private long tHybrid = -1;


    private int trainingEpoch;


    private int tdSteps;
    private List<Float> legalActionMaxEntropies;

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
        this.legalActionMaxEntropies = new ArrayList<>();
        this.rootValuesFromInitialInference = new ArrayList<>();
        this.rootEntropyValuesFromInitialInference = new ArrayList<>();
        this.surprised = false;
        this.hybrid = false;
    }

    public boolean hasExploration() {
        return tHybrid > 0;
    }
//    public double getAverageEntropyFromInitialInference() {
//        return IntStream.range(0, rootEntropyValuesFromInitialInference.size())
//                .mapToDouble(i -> rootEntropyValuesFromInitialInference.get(i))
//                .sum() / Math.max(1, rootEntropyValuesFromInitialInference.size());
//    }
    public double getAverageEntropy() {
        return IntStream.range(0, entropies.size())
                .mapToDouble(i -> entropies.get(i))
                .sum() / Math.max(1, entropies.size());
    }

    public double getAverageActionMaxEntropy() {
        return IntStream.range(0, legalActionMaxEntropies.size())
                .mapToDouble(i -> legalActionMaxEntropies.get(i))
                .sum() / Math.max(1, legalActionMaxEntropies.size());
    }


    public GameDTO copy(int toPosition) {
        GameDTO copy = new GameDTO();

        copy.networkName = this.networkName;
        copy.surprised = this.surprised;
        copy.hybrid = this.hybrid;
        copy.tHybrid = this.tHybrid;
        copy.tdSteps = this.tdSteps;
        copy.trainingEpoch = this.trainingEpoch;
        copy.nextSurpriseCheck = this.nextSurpriseCheck;
        copy.pRandomActionRawSum = this.pRandomActionRawSum;
        copy.pRandomActionRawCount = this.pRandomActionRawCount;
        if (toPosition > 0) {
            copy.rewards.addAll(this.rewards.subList(0, toPosition));
            copy.entropies.addAll(this.entropies.subList(0, toPosition));
            copy.legalActionMaxEntropies.addAll(this.legalActionMaxEntropies.subList(0, toPosition));
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
        copy.tHybrid = this.tHybrid;
        copy.tdSteps = this.tdSteps;
        copy.trainingEpoch = this.trainingEpoch;
        copy.entropies.addAll(this.entropies);
        copy.legalActionMaxEntropies.addAll(this.legalActionMaxEntropies);
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
        final int observationPartSize = observations.isEmpty() ? 0 : observations.get(0).getPartSize();


        GameProto.Builder gameBuilder = GameProto.newBuilder();
        gameBuilder.setNetworkName(this.networkName);
        gameBuilder.setLastValueError(this.lastValueError);
        gameBuilder.setCount(this.count);
        gameBuilder.setNextSurpriseCheck(this.nextSurpriseCheck);
        gameBuilder.setSurprised(this.surprised);
        gameBuilder.setHybrid(this.hybrid);
        gameBuilder.setTHybrid(this.tHybrid);
        gameBuilder.setTdSteps(this.tdSteps);
        gameBuilder.setTrainingEpoch(this.trainingEpoch);
        gameBuilder.addAllActions(getActions());
        gameBuilder.setPRandomActionRawSum(this.pRandomActionRawSum);
        gameBuilder.setPRandomActionRawCount(this.pRandomActionRawCount);
        gameBuilder.addAllRewards(getRewards());
        gameBuilder.addAllRootValueTargets(getRootValueTargets());
        gameBuilder.addAllRootEntropyValueTargets(getRootEntropyValueTargets());
        gameBuilder.addAllEntropies(getEntropies());
        gameBuilder.addAllMaxEntropies(getLegalActionMaxEntropies());
        gameBuilder.addAllRootValuesFromInitialInference(getRootValuesFromInitialInference());
        gameBuilder.addAllRootEntropyValuesFromInitialInference(getRootEntropyValuesFromInitialInference());
        getPlayoutPolicy().forEach(policy -> {
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
            b.setObservationPartA(observation.toByteStringA());
            b.setObservationPartB(observation.toByteStringB());
            b.setObservationPartSize(observationPartSize);
            b.setTwoPlayer(observation instanceof ObservationTwoPlayers);
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
        this.setTHybrid(p.getTHybrid());
        this.setTdSteps(p.getTdSteps());
        this.setTrainingEpoch(p.getTrainingEpoch());

        this.setActions(p.getActionsList());
        this.setPRandomActionRawSum(p.getPRandomActionRawSum());
        this.setPRandomActionRawCount(p.getPRandomActionRawCount());
        this.setRewards(p.getRewardsList());
        this.setRootValueTargets(p.getRootValueTargetsList());
        this.setRootEntropyValueTargets(p.getRootEntropyValueTargetsList());
        this.setEntropies(p.getEntropiesList());
        this.setLegalActionMaxEntropies(p.getMaxEntropiesList());
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
                            .map(o -> {
                                if (o.getTwoPlayer()) {
                                    return ObservationTwoPlayers.fromByteStringAndPartSize(o.getObservationPartA(), o.getObservationPartB(), o.getObservationPartSize());
                                } else {
                                    return ObservationOnePlayer.fromByteStringAndPartSize(o.getObservationPartA(), o.getObservationPartSize());
                                }

                            })
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

    public boolean deepEquals(GameDTO gameDTO) {
        // implement an equals method that compares all fields
        return this.networkName.equals(gameDTO.getNetworkName())
          //      && this.tSurprise == gameDTO.getTSurprise()
                && this.tHybrid == gameDTO.getTHybrid()
//                && this.tStateA == gameDTO.getTStateA()
//                && this.tStateB == gameDTO.getTStateB()
                && this.tdSteps == gameDTO.getTdSteps()
                && this.trainingEpoch == gameDTO.getTrainingEpoch()
                //     && this.observationPartSize == gameDTO.getObservationPartSize()
                && this.actions.equals(gameDTO.getActions())
                && this.pRandomActionRawSum == gameDTO.getPRandomActionRawSum()
                && this.pRandomActionRawCount == gameDTO.getPRandomActionRawCount()
                && this.rewards.equals(gameDTO.getRewards())
                && this.rootValueTargets.equals(gameDTO.getRootValueTargets())
                && this.rootEntropyValueTargets.equals(gameDTO.getRootEntropyValueTargets())
                && this.entropies.equals(gameDTO.getEntropies())
                && this.legalActionMaxEntropies.equals(gameDTO.getLegalActionMaxEntropies())
                && this.rootValuesFromInitialInference.equals(gameDTO.getRootValuesFromInitialInference())
                && this.rootEntropyValuesFromInitialInference.equals(gameDTO.getRootEntropyValuesFromInitialInference())
                && this.count == gameDTO.getCount()
                && this.nextSurpriseCheck == gameDTO.getNextSurpriseCheck()
                // finish and remind that policyTargets is a list of float arrays
                && this.policyTargets.size() == gameDTO.getPolicyTargets().size()
                && this.observations.size() == gameDTO.getObservations().size()
                && this.legalActions.size() == gameDTO.getLegalActions().size()
                && this.playoutPolicy.size() == gameDTO.getPlayoutPolicy().size()
                // also compare for content of the arrays
                && this.policyTargets.stream().allMatch(
                policy -> gameDTO.getPolicyTargets().stream().anyMatch(
                        policy2 -> Arrays.equals(policy, policy2)
                )
        )
                && this.observations.stream().allMatch(
                observation -> gameDTO.getObservations().stream().anyMatch(
                        observation::equals
                )
        )
                && this.legalActions.stream().allMatch(
                legalAction -> gameDTO.getLegalActions().stream().anyMatch(
                        legalAction2 -> Arrays.equals(legalAction, legalAction2)
                )
        )
                && this.playoutPolicy.stream().allMatch(
                playoutPolicy_ -> gameDTO.getPlayoutPolicy().stream().anyMatch(
                        playoutPolicy2 -> Arrays.equals(playoutPolicy_, playoutPolicy2)
                )
        )
                && this.lastValueError == gameDTO.getLastValueError()
                && this.surprised == gameDTO.isSurprised()

                && this.hybrid == gameDTO.isHybrid();
    }

}

