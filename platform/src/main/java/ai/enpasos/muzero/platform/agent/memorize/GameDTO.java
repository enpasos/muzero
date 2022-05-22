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
import ai.enpasos.muzero.platform.agent.memory.protobuf.ValueProtos;
import ai.enpasos.muzero.platform.common.MuZeroException;
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
public class GameDTO {

    @EqualsAndHashCode.Include
    private List<Integer> actions;

    private List<Float> rewards;
    private List<Float> surprises;
    private List<float[]> policyTargets;

    private List<List<Float>> values;
    private List<Float> rootValues;
    private List<Float> rootValuesFromInitialInference;
    // private List<Float> entropies;
    private float lastValueError;
    private long count;

    private boolean surprised;
    private long tSurprise;
    private long tStateA;
    private long tStateB;

    public GameDTO() {
        this.actions = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.policyTargets = new ArrayList<>();
        this.rootValues = new ArrayList<>();
        // this.entropies = new ArrayList<>();
        this.surprises = new ArrayList<>();
        this.rootValuesFromInitialInference = new ArrayList<>();
        this.values = new ArrayList<>();
        this.surprised = false;
    }

    public @NotNull String getActionHistoryAsString() {
        StringBuilder buf = new StringBuilder(this.getActions().size());
        this.actions.forEach(a -> buf.append(a.intValue()).append("."));
        return buf.toString();
    }

    public GameDTO copy(int toPosition) {
        toPosition = Math.max(1, toPosition);
        GameDTO copy = new GameDTO();
        copy.rewards.addAll(this.rewards.subList(0, toPosition));
        copy.surprised = this.surprised;
        copy.tSurprise = this.tSurprise;
        copy.tStateA = this.tStateA;
        copy.tStateB = this.tStateB;
        copy.count = this.count;
        copy.surprises.addAll(this.surprises.subList(0, toPosition));
        copy.actions.addAll(this.actions.subList(0, toPosition));
        if (this.policyTargets.size() < toPosition) {
            int k = 42; // should not happen
        }
        this.policyTargets.subList(0, toPosition).forEach(pT -> copy.policyTargets.add(Arrays.copyOf(pT, pT.length)));
        //this.values.subList(0, toPosition).forEach(pT -> copy.values.add(List.copyOf(pT)));
        if (this.rootValues.size() >= toPosition)
            copy.rootValues.addAll(this.rootValues.subList(0, toPosition));
        if (this.rootValuesFromInitialInference.size() >= toPosition)
            copy.rootValuesFromInitialInference.addAll(this.rootValuesFromInitialInference.subList(0, toPosition));
        return copy;
    }

    public GameDTO copy() {
        GameDTO copy = new GameDTO();
        copy.count = this.count;
        copy.rewards.addAll(this.rewards);
        copy.surprised = this.surprised;
        copy.tSurprise = this.tSurprise;
        copy.tStateA = this.tStateA;
        copy.tStateB = this.tStateB;
        copy.surprises.addAll(this.surprises);
        copy.actions.addAll(this.actions);
        this.policyTargets.forEach(pT -> copy.policyTargets.add(Arrays.copyOf(pT, pT.length)));
        this.values.forEach(pT -> copy.values.add(List.copyOf(pT)));
        copy.rootValues.addAll(this.rootValues);
        copy.rootValuesFromInitialInference.addAll(this.rootValuesFromInitialInference);
        return copy;
    }

    public GameProto proto() {
        GameProto.Builder gameBuilder = GameProto.newBuilder();
        gameBuilder.setLastValueError(this.lastValueError);
        gameBuilder.setCount(this.count);
        gameBuilder.setSurprised(this.surprised);
        gameBuilder.setTSurprise(this.tSurprise);
        gameBuilder.setTStateA(this.tStateA);
        gameBuilder.setTStateB(this.tStateB);
        gameBuilder.addAllActions(getActions());
        gameBuilder.addAllRewards(getRewards());
        gameBuilder.addAllRootValues(getRootValues());
        //  gameBuilder.addAllEntropies(getEntropies());
        gameBuilder.addAllSurprises(getSurprises());
        gameBuilder.addAllRootValuesFromInitialInference(getRootValuesFromInitialInference());

        getPolicyTargets().stream().forEach(policyTarget -> {
            PolicyTargetProtos.Builder b = PolicyTargetProtos.newBuilder();
            IntStream.range(0, policyTarget.length).forEach(i ->
                b.addPolicyTarget(policyTarget[i])
            );
            gameBuilder.addPolicyTargets(b.build());
        });
        // TODO value saving switched of for now because of performance problem
//        getValues().stream().forEach(v -> {
//            ValueProtos.Builder b = ValueProtos.newBuilder();
//            b.addAllValue(v);
//            gameBuilder.addValues(b.build());
//        });
        return gameBuilder.build();
    }

    public void deproto(GameProto p) {
        this.setSurprised(p.getSurprised());
        this.setTSurprise(p.getTSurprise());
        this.setTStateA(p.getTStateA());
        this.setTStateB(p.getTStateB());
        this.setActions(p.getActionsList());
        this.setRewards(p.getRewardsList());
        this.setRootValues(p.getRootValuesList());
        //   this.setEntropies(p.getEntropiesList());
        this.setSurprises(p.getSurprisesList());
        this.setLastValueError(p.getLastValueError());
        this.setRootValuesFromInitialInference(p.getRootValuesFromInitialInferenceList());
        this.setCount(p.getCount());


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
        if (p.getValuesCount() > 0) {
            List<List<Float>> vs = p.getValuesList().stream().map(
                    valueProtos -> {
                        //valueProtos.getValueList();
                        return List.copyOf(valueProtos.getValueList());
                    })
                .collect(Collectors.toList());
            this.setValues(vs);
        }
        int i = 42;
    }



//    public void removeTimeSteps(int backInTime) {
//        IntStream.range(0, backInTime).forEach(i -> {
//            this.getSurprises().remove(this.getSurprises().size()-1);
//            this.getPolicyTargets().remove(this.getPolicyTargets().size()-1);
//            this.getRewards().remove(this.getRewards().size()-1);
//            this.getActions().remove(this.getActions().size()-1);
//            if(this.getValues() != null && !this.getValues().isEmpty())
//                this.getValues().remove(this.getValues().size()-1);
//            if (this.getRootValues() != null && !this.getRootValues().isEmpty())
//                this.getRootValues().remove(this.getRootValues().size()-1);
//            if (this.getRootValuesFromInitialInference() != null && !this.getRootValuesFromInitialInference().isEmpty())
//                this.getRootValuesFromInitialInference().remove(this.getRootValuesFromInitialInference().size()-1);
//        });
//    }
}

