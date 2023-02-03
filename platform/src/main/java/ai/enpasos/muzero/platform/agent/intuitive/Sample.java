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

package ai.enpasos.muzero.platform.agent.intuitive;

import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.GameBuffer;
import ai.enpasos.muzero.platform.agent.memorize.Target;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
    private List<Observation> observations = new ArrayList<>();


    @Builder.Default
    private List<Integer> actionsList = new ArrayList<>();

    @Builder.Default
    private List<Target> targetList = new ArrayList<>();


    private GameBuffer gameBuffer;
    private Game game;
    private int gamePos;
    private int numUnrollSteps;

    public void makeTarget(double pRatioMax) {
        targetList = game.makeTarget(pRatioMax, gamePos, numUnrollSteps );
    }

    public double getPRatioMax() {
       //  GameBuffer gameBuffer, int currentIndex, int T)
        int currentIndex =  gamePos;
        int T = game.getGameDTO().getActions().size() - 1;
            int tdSteps;
            tdSteps = 0;
//            if (!config.offPolicyCorrectionOn()) return tdSteps;
            if (game.getGameDTO().getPlayoutPolicy().isEmpty()) return 1;

            double pRatioMax = 0;

            double b = ThreadLocalRandom.current().nextDouble(0, 1);

            for (int t = T; t >= currentIndex; t--) {

                double pBase = 1;
                for (int i = t; i <= T; i++) {
                    pBase *=  game.getGameDTO().getPlayoutPolicy().get(i)[game.getGameDTO().getActions().get(i)];
                }
                double p = 1;
                for (int i = t; i <= T; i++) {
                    p *= game.getGameDTO().getPolicyTargets().get(i)[game.getGameDTO().getActions().get(i)];
                }
                double pRatio = p / pBase;
                   if (pRatio > pRatioMax) {
                       pRatioMax = pRatio;
                   }
            }

        return pRatioMax;
    }
}
