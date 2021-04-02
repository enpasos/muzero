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

package ai.enpasos.muzero;

import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.network.djl.TrainingHelper;
import ai.enpasos.muzero.play.PlayManager;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.MuZero.deleteNetworksAndGames;
import static ai.enpasos.muzero.network.djl.TrainingHelper.trainAndReturnNumberOfLastTrainingStep;

class MuZeroSmokeTest {

    public static void createRandomGamesForOneBatch(MuZeroConfig config) {
        deleteNetworksAndGames(config);
        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        PlayManager.playParallel(replayBuffer, config, config.getBatchSize(), true, true, false, 1);


    }

    @Test
    void smoketest() throws Exception {
        MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();
        config.setOutputDir("target/smoketest/");
        config.setNumberOfTrainingStepsPerEpoch(1);

        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        replayBuffer.loadLatestState();

        TrainingHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 0);

        PlayManager.playParallel(replayBuffer, config, 1, true, false, false, 1);
        PlayManager.playParallel(replayBuffer, config, 5, false, false, false, 100);
        replayBuffer.saveState();
        TrainingHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 1);


    }
}
