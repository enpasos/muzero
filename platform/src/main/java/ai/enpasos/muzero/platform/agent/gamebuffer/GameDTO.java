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

package ai.enpasos.muzero.platform.agent.gamebuffer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GameDTO  {

    @EqualsAndHashCode.Include
    private List<Integer> actionHistory;

    private List<Float> rewards;
    private List<float[]> policyTarget;
    private List<Float> rootValues;

    public GameDTO(@NotNull Game game) {
        this.setActionHistory(new ArrayList<>());
        this.rewards = new ArrayList<>();
        this.policyTarget = new ArrayList<>();
        this.rootValues = new ArrayList<>();
    }

    public @NotNull String getActionHistoryAsString() {
        StringBuilder buf = new StringBuilder(this.getActionHistory().size());
        this.getActionHistory().forEach(a -> buf.append(a.intValue()).append("."));
        return buf.toString();
    }

    public GameDTO copy() {
        GameDTO copy = new GameDTO();
        copy.rewards.addAll(this.rewards);
        copy.actionHistory.addAll(this.actionHistory);
        this.policyTarget.forEach(pT -> copy.policyTarget.add(Arrays.copyOf(pT, pT.length)));
        copy.rootValues.addAll(this.rootValues);
        return copy;
    }
}
