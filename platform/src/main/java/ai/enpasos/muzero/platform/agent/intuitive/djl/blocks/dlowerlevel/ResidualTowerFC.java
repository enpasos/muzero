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

package ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.dlowerlevel;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S110")
public class ResidualTowerFC extends MySequentialBlock {

    private ResidualTowerFC() {
    }

    @Builder()
    public static @NotNull ResidualTowerFC newResidualTower(int numResiduals, int numChannels) {
        ResidualTowerFC instance = new ResidualTowerFC();
        for (int i = 0; i < numResiduals; i++) {
            instance.add(new ResidualBlockFC(numChannels));
        }
        return instance;
    }

}
