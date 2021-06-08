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
import ai.enpasos.muzero.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.agent.slow.play.PlayManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.MuZero.deleteNetworksAndGames;

@Slf4j
class MuZeroSmokeTest {

    public static void createRandomGamesForOneBatch(@NotNull MuZeroConfig config) {
        deleteNetworksAndGames(config);
        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        PlayManager.playParallel(replayBuffer, config, config.getBatchSize(), true, true, 1);


    }

    @Test
    void smoketest() {
        try {
            MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();
            config.setOutputDir("target/smoketest/");
            config.setNumberOfTrainingStepsPerEpoch(1);

            ReplayBuffer replayBuffer = new ReplayBuffer(config);
            replayBuffer.loadLatestState();

            NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 0);

            PlayManager.playParallel(replayBuffer, config, 1, true, false, 1);
            PlayManager.playParallel(replayBuffer, config, 5, false, false, 100);
            replayBuffer.saveState();
            NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 1);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

    }
}
