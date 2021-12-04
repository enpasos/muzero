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

package ai.enpasos.muzero.tictactoe;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.MuZero;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.slow.play.PlayManager;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.tictactoe.config.TicTacToeConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@Slf4j
@Ignore
// cpu memory grows beyond 64 GB, but integration test works (came about 30/11/2021 on djl master, before was ok)
public class SmokeTest {

    public static void createRandomGamesForOneBatch(@NotNull MuZeroConfig config) {
        MuZero.deleteNetworksAndGames(config);
        ReplayBuffer replayBuffer = new ReplayBuffer(config);

        try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
            Network network = new Network(config, model);
            PlayManager.playParallel(network, replayBuffer, config, true, true, true);
        }


    }

    @Test
    public void smoketest() {

        MuZeroConfig config = TicTacToeConfigFactory.getTicTacToeInstance();
        try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
            Network network = new Network(config, model);
            config.setOutputDir("target/smoketest/");
            config.setNumberOfTrainingStepsPerEpoch(1);

            ReplayBuffer replayBuffer = new ReplayBuffer(config);
            replayBuffer.loadLatestState();

            NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 0);

            PlayManager.playParallel(network, replayBuffer, config, true, false, true);

            PlayManager.playParallel(network, replayBuffer, config, false, false, true);
            replayBuffer.saveState();
            NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 1);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

    }
}
