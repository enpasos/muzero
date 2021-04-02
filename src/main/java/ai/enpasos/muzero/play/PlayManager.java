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

package ai.enpasos.muzero.play;

import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.MuZero;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.gamebuffer.GameIO;
import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.network.Network;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class PlayManager {

    public static void play(ReplayBuffer replayBuffer, MuZeroConfig config, int numberOfPlays, boolean render, boolean fastRuleLearning, boolean persistPerGame, int noGamesParallel) {
        playParallel(replayBuffer, config, numberOfPlays, render, fastRuleLearning, persistPerGame, 1);
    }

    public static void playParallel(ReplayBuffer replayBuffer, MuZeroConfig config, int numberOfPlays, boolean render, boolean fastRuleLearning, boolean persistPerGame, int noGamesParallel) {

        for (int i = 0; i < numberOfPlays; i++) {
            try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {
                List<NDArray> actionSpaceOnDevice = getAllActionsOnDevice(config, model.getNDManager());
                Network network = null;
                if (!fastRuleLearning)
                    network = new Network(config, model);

                List<Game> gameList = SelfPlayParallel.playGame(config, network, render, fastRuleLearning, noGamesParallel, actionSpaceOnDevice);
                gameList.forEach(game -> replayBuffer.saveGame(game));

            }

            log.info("Played {} games parallel, round {}", noGamesParallel, i);
        }

    }

    public static List<NDArray> getAllActionsOnDevice(MuZeroConfig config, NDManager ndManager) {
        List<Action> actions = config.newGame().allActionsInActionSpace();
        return actions.stream().map(action -> action.encode(ndManager)).collect(Collectors.toList());
    }


    private static void saveGame(ReplayBuffer replayBuffer, Game game, MuZeroConfig config, boolean persistPerGame) {

        replayBuffer.saveGame(game);

        if (persistPerGame) {
            byte[] gameData = game.encode();
            String pathname = MuZero.getGamesBasedir(config) + "/game" + (GameIO.getNewLatestGameNo(config));
            System.out.println("saving ... " + pathname);

            try {
                FileUtils.writeByteArrayToFile(new File(pathname),
                        gameData);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
