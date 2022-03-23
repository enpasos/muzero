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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GameDTO {

    @EqualsAndHashCode.Include
    private List<Integer> actions;

    private List<Float> rewards;
    private List<float[]> policyTargets;
    private List<Float> rootValues;
    private List<Float> rootValuesFromInitialInference;
    private List<Float> entropies;
    private float lastValueError;

    public GameDTO() {
        this.actions = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.policyTargets = new ArrayList<>();
        this.rootValues = new ArrayList<>();
        this.entropies = new ArrayList<>();
        this.rootValuesFromInitialInference = new ArrayList<>();
    }

    public @NotNull String getActionHistoryAsString() {
        StringBuilder buf = new StringBuilder(this.getActions().size());
        this.actions.forEach(a -> buf.append(a.intValue()).append("."));
        return buf.toString();
    }

    public GameDTO copy() {
        GameDTO copy = new GameDTO();
        copy.rewards.addAll(this.rewards);
        copy.actions.addAll(this.actions);
        this.policyTargets.forEach(pT -> copy.policyTargets.add(Arrays.copyOf(pT, pT.length)));
        copy.rootValues.addAll(this.rootValues);
        return copy;
    }

    public GameProto proto() {
        GameProto.Builder gameBuilder = GameProto.newBuilder();
        gameBuilder.setLastValueError(this.lastValueError);
        gameBuilder.addAllActions(getActions());
        gameBuilder.addAllRewards(getRewards());
        gameBuilder.addAllRootValues(getRootValues());
        gameBuilder.addAllEntropies(getEntropies());
        gameBuilder.addAllRootValuesFromInitialInference(getRootValuesFromInitialInference());

        getPolicyTargets().stream().forEach(policyTarget -> {
            PolicyTargetProtos.Builder b = PolicyTargetProtos.newBuilder();
            IntStream.range(0, policyTarget.length).forEach(i ->
                b.addPolicyTarget(policyTarget[i])
            );
            gameBuilder.addPolicyTargets(b.build());
        });
        return gameBuilder.build();
    }

    public void deproto(GameProto p) {
        this.setActions(p.getActionsList());
        this.setRewards(p.getRewardsList());
        this.setRootValues(p.getRootValuesList());
        this.setEntropies(p.getEntropiesList());
        this.setLastValueError(p.getLastValueError());
        this.setRootValuesFromInitialInference(p.getRootValuesFromInitialInferenceList());


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
}
