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

package ai.enpasos.muzero.platform.agent.memorize;

import ai.enpasos.muzero.platform.agent.memory.protobuf.GameProto;
import ai.enpasos.muzero.platform.agent.memory.protobuf.PolicyTargetProtos;
import ai.enpasos.muzero.platform.config.TrainingTypeKey;
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
    private List<Float> surprises;
    private List<float[]> policyTargets;
    private List<Float> rootValueTargets;
    private List<Float> rootValuesFromInitialInference;
    private float lastValueError;
    private long count;
    private long nextSurpriseCheck;
    private boolean surprised;
    private long tSurprise;
    private long tStateA;
    private long tStateB;


    private int tdSteps;

    public GameDTO(List<Integer> actions) {
        this();
        this.actions.addAll(actions);
    }

    public GameDTO() {
        this.actions = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.policyTargets = new ArrayList<>();
        this.rootValueTargets = new ArrayList<>();
        this.surprises = new ArrayList<>();
        this.rootValuesFromInitialInference = new ArrayList<>();
        this.surprised = false;
    }

    public @NotNull String getActionHistoryAsString() {
        StringBuilder buf = new StringBuilder(this.getActions().size());
        this.actions.forEach(a -> buf.append(a.intValue()).append("."));
        return buf.toString();
    }

    public GameDTO copy(int toPosition) {
        GameDTO copy = new GameDTO();

        copy.networkName = this.networkName;
        copy.surprised = this.surprised;
        copy.tSurprise = this.tSurprise;
        copy.tStateA = this.tStateA;
        copy.tStateB = this.tStateB;
        copy.tdSteps = this.tdSteps;
        copy.count = this.count;
        copy.nextSurpriseCheck = this.nextSurpriseCheck;
        copy.pRandomActionRawSum = this.pRandomActionRawSum;
        copy.pRandomActionRawCount = this.pRandomActionRawCount;
        if (toPosition > 0) {
            copy.rewards.addAll(this.rewards.subList(0, toPosition));
            copy.surprises.addAll(this.surprises.subList(0, toPosition));
            copy.actions.addAll(this.actions.subList(0, toPosition));

            this.policyTargets.subList(0, toPosition).forEach(pT -> copy.policyTargets.add(Arrays.copyOf(pT, pT.length)));
            if (this.rootValueTargets.size() >= toPosition)
                copy.rootValueTargets.addAll(this.rootValueTargets.subList(0, toPosition));
            if (this.rootValuesFromInitialInference.size() >= toPosition)
                copy.rootValuesFromInitialInference.addAll(this.rootValuesFromInitialInference.subList(0, toPosition));
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
        copy.tSurprise = this.tSurprise;
        copy.tStateA = this.tStateA;
        copy.tStateB = this.tStateB;
        copy.tdSteps = this.tdSteps;
        copy.surprises.addAll(this.surprises);
        copy.actions.addAll(this.actions);

        copy.pRandomActionRawSum = this.pRandomActionRawSum;
        copy.pRandomActionRawCount = this.pRandomActionRawCount;
        this.policyTargets.forEach(pT -> copy.policyTargets.add(Arrays.copyOf(pT, pT.length)));
        copy.rootValueTargets.addAll(this.rootValueTargets);
        copy.rootValuesFromInitialInference.addAll(this.rootValuesFromInitialInference);
        return copy;
    }

    public GameProto proto() {
        GameProto.Builder gameBuilder = GameProto.newBuilder();
        gameBuilder.setNetworkName(this.networkName);
        gameBuilder.setLastValueError(this.lastValueError);
        gameBuilder.setCount(this.count);
        gameBuilder.setNextSurpriseCheck(this.nextSurpriseCheck);
        gameBuilder.setSurprised(this.surprised);
        gameBuilder.setTSurprise(this.tSurprise);
        gameBuilder.setTStateA(this.tStateA);
        gameBuilder.setTStateB(this.tStateB);
        gameBuilder.setTdSteps(this.tdSteps);
        gameBuilder.addAllActions(getActions());
        gameBuilder.setPRandomActionRawSum(this.pRandomActionRawSum);
        gameBuilder.setPRandomActionRawCount(this.pRandomActionRawCount);
        gameBuilder.addAllRewards(getRewards());
        gameBuilder.addAllRootValueTargets(getRootValueTargets());
        gameBuilder.addAllSurprises(getSurprises());
        gameBuilder.addAllRootValuesFromInitialInference(getRootValuesFromInitialInference());

        getPolicyTargets().forEach(policyTarget -> {
            PolicyTargetProtos.Builder b = PolicyTargetProtos.newBuilder();
            IntStream.range(0, policyTarget.length).forEach(i ->
                b.addPolicyTarget(policyTarget[i])
            );
            gameBuilder.addPolicyTargets(b.build());
        });
        return gameBuilder.build();
    }

    public void deproto(GameProto p) {
        this.setNetworkName(p.getNetworkName());
        this.setSurprised(p.getSurprised());
        this.setTSurprise(p.getTSurprise());
        this.setTStateA(p.getTStateA());
        this.setTStateB(p.getTStateB());
        this.setTdSteps(p.getTdSteps());
        this.setActions(p.getActionsList());
        this.setPRandomActionRawSum(p.getPRandomActionRawSum());
        this.setPRandomActionRawCount(p.getPRandomActionRawCount());
        this.setRewards(p.getRewardsList());
        this.setRootValueTargets(p.getRootValueTargetsList());
        this.setSurprises(p.getSurprisesList());
        this.setLastValueError(p.getLastValueError());
        this.setRootValuesFromInitialInference(p.getRootValuesFromInitialInferenceList());
        this.setCount(p.getCount());
        this.setNextSurpriseCheck(p.getNextSurpriseCheck());


        if (p.getPolicyTargetsCount() > 0) {
            this.setPolicyTargets(p.getPolicyTargetsList().stream().map(policyTargetProtos -> {
                        float[] result = new float[p.getPolicyTargets(0).getPolicyTargetCount()];
                        int i = 0;
                        for (Float f : policyTargetProtos.getPolicyTargetList()) {
                            result[i++] = f;
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

