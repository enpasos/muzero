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

package ai.enpasos.muzero.platform.agent.d_model;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.Target;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sample {

    @Builder.Default
    private boolean actionTrainingPlayerA = true;

    @Builder.Default
    private boolean actionTrainingPlayerB = true;


    @Builder.Default
    private List<ObservationModelInput> observations = new ArrayList<>();


    @Builder.Default
    private List<Integer> actionsList = new ArrayList<>();

    @Builder.Default
    private List<Target> targetList = new ArrayList<>();


    private GameBuffer gameBuffer;
    private Game game;
    private int gamePos;
    private int numUnrollSteps;

    public void makeTarget(boolean withLegalActionHead) {
        targetList = game.makeTarget(gamePos, numUnrollSteps,  withLegalActionHead );
    }


}
