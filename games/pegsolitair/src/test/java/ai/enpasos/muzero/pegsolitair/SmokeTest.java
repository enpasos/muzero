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

package ai.enpasos.muzero.pegsolitair;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.MuZero;
import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.slow.play.PlayManager;
import ai.enpasos.muzero.platform.agent.slow.play.ThinkBudget;
import ai.enpasos.muzero.platform.agent.slow.play.ThinkConf;
import ai.enpasos.muzero.pegsolitair.config.PegSolitairConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@Slf4j
public class SmokeTest {

    public static void createRandomGamesForOneBatch(@NotNull MuZeroConfig config) {
        MuZero.deleteNetworksAndGames(config);
        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        ThinkConf thinkConf = ThinkConf.builder()

                .playerAConfig(
                        ThinkBudget.builder()
                                .numSims(config.getNumSimulations())
                                .numParallel(1)
                                .numOfPlays(config.getBatchSize())
                                .build())
                .playerBConfig(
                        ThinkBudget.builder()
                                .numSims(config.getNumSimulations())
                                .numParallel(1)
                                .numOfPlays(config.getBatchSize())
                                .build())
                .build();

        try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
            PlayManager.playParallel(model, replayBuffer, config, true, true, thinkConf, true);
        }

    }

    @Test

    @Ignore
    public void smoketest() {
        try {
            MuZeroConfig config = PegSolitairConfigFactory.getSolitairInstance();
            config.setOutputDir("target/smoketest/");
            config.setNumberOfTrainingStepsPerEpoch(1);

            ReplayBuffer replayBuffer = new ReplayBuffer(config);
            replayBuffer.loadLatestState();

            NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 0);
            ThinkConf thinkConf = ThinkConf.builder()

                    .playerAConfig(
                            ThinkBudget.builder()
                                    .numSims(config.getNumSimulations())
                                    .numParallel(1)
                                    .numOfPlays(1)
                                    .build())
                    .playerBConfig(
                            ThinkBudget.builder()
                                    .numSims(config.getNumSimulations())
                                    .numParallel(1)
                                    .numOfPlays(1)
                                    .build())
                    .build();
            try (Model model = Model.newInstance(config.getModelName(), Device.cpu())) {
                PlayManager.playParallel(model, replayBuffer, config, true, false, thinkConf, true);

            thinkConf = ThinkConf.builder()

                    .playerAConfig(
                            ThinkBudget.builder()
                                    .numSims(config.getNumSimulations())
                                    .numParallel(3)
                                    .numOfPlays(2)
                                    .build())
                    .playerBConfig(
                            ThinkBudget.builder()
                                    .numSims(config.getNumSimulations())
                                    .numParallel(3)
                                    .numOfPlays(2)
                                    .build())
                    .build();
            PlayManager.playParallel(model, replayBuffer, config, false, false, thinkConf, true);

            replayBuffer.saveState();
            NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 1);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

    }
}
