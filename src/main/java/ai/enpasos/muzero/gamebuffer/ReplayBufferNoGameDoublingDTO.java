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
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.LinkedHashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplayBufferNoGameDoublingDTO implements Serializable {

    private final LinkedHashMap<String, GameDTO> data = new LinkedHashMap<>();
    private long counter;
    private int windowSize;

    public ReplayBufferNoGameDoublingDTO(int windowSize) {
        this.windowSize = windowSize;
    }

    public void saveGame(@NotNull GameDTO gameDTO) {

        String key = gameDTO.getActionHistoryAsString();
        data.remove(key);
        while (data.size() >= windowSize) {
            String firstKey = data.keySet().iterator().next();
            data.remove(firstKey);
        }
        data.put(gameDTO.getActionHistoryAsString(), gameDTO);
        counter++;
    }
}
