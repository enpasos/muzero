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

import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.EnvironmentBase;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;

@Slf4j
public class TestEnvironment extends EnvironmentBase {


    public TestEnvironment(@NotNull MuZeroConfig config) {
        super(config);
    }


    public float step(Action action) {
        return 0f;
    }

    @Override
    public Observation getObservation() {
        return ObservationTwoPlayers.builder()
                .partA(BitSet.valueOf(new byte[0]))
                .partB(BitSet.valueOf(new byte[0]))
                .build();
    }
}
