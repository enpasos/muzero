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

import ai.enpasos.muzero.gamebuffer.GameIO;
import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.network.djl.TrainingHelper;
import ai.enpasos.muzero.play.PlayManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

@Slf4j
public class MuZero {


    public static void main(String[] args) {

        MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();

        createNetworkModelIfNotExisting(config);

        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        replayBuffer.loadLatestState();

        int trainingStep = 0;

        do {

            PlayManager.playParallel(replayBuffer, config, 1, true, false, false, 1);
            PlayManager.playParallel(replayBuffer, config, 4, false, false, false, 1000);
            replayBuffer.saveState();
            trainingStep = TrainingHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 1);
            log.info("last training step = {}", trainingStep);


        } while (trainingStep < config.getNumberOfTrainingSteps());


    }

    private static void createNetworkModelIfNotExisting(MuZeroConfig config) {
        TrainingHelper.trainAndReturnNumberOfLastTrainingStep(config, null, 0);
    }


    private static void makeObjectDir(MuZeroConfig config) {
        try {
            FileUtils.forceMkdir(new File(getNetworksBasedir(config) + "/" + (GameIO.getLatestObjectNo(config) + 1)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteNetworksAndGames(MuZeroConfig config) {
        try {
            FileUtils.forceDelete(new File(config.getOutputDir()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileUtils.forceMkdir(new File(getNetworksBasedir(config)));
            FileUtils.forceMkdir(new File(getGamesBasedir(config)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String getGamesBasedir(MuZeroConfig config) {
        return config.getOutputDir() + "games";
    }


    public static String getNetworksBasedir(MuZeroConfig config) {
        return config.getOutputDir() + "networks";
    }


}
