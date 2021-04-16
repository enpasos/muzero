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

package ai.enpasos.muzero.gamebuffer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GameDTO implements Serializable {

    String gameClassName;

    @EqualsAndHashCode.Include
    List<Integer> actionHistory;

    List<Float> rewards;
    List<float[]> childVisits;
    List<Float> rootValues;

    public GameDTO(@NotNull Game game) {
        this.gameClassName = game.getClass().getCanonicalName();
        this.actionHistory = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.childVisits = new ArrayList<>();
        this.rootValues = new ArrayList<>();
    }


    public @NotNull String getActionHistoryAsString() {
        StringBuffer buf = new StringBuffer(this.actionHistory.size());
        this.actionHistory.forEach(a -> buf.append(a.intValue()).append("."));
        return buf.toString();
    }
}
