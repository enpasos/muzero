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

package ai.enpasos.muzero.platform.agent.slow.play;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.MuZero;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.gamebuffer.GameIO;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
public class PlayManager {


    public static void playParallel(Network network, @NotNull ReplayBuffer replayBuffer, @NotNull MuZeroConfig config, boolean render, boolean fastRuleLearning, boolean explorationNoise) {

        for (int i = 0; i < config.getNumPlays(); i++) {

            List<Game> gameList = SelfPlay.playGame(config, network, render, fastRuleLearning, explorationNoise);
            gameList.forEach(replayBuffer::saveGame);

            log.info("Played {} games parallel, round {}", config.getNumParallelPlays(), i);
        }

    }

    public static List<NDArray> getAllActionsOnDevice(@NotNull MuZeroConfig config, @NotNull NDManager ndManager) {
        List<Action> actions = Objects.requireNonNull(config.newGame()).allActionsInActionSpace();
        return actions.stream().map(action -> action.encode(ndManager)).collect(Collectors.toList());
    }


//    private static void saveGame(@NotNull ReplayBuffer replayBuffer, @NotNull Game game, @NotNull MuZeroConfig config, boolean persistPerGame) {
//
//        replayBuffer.saveGame(game);
//
//        if (persistPerGame) {
//            byte[] gameData = game.encode();
//            String pathname = MuZero.getGamesBasedir(config) + "/game" + (GameIO.getNewLatestGameNo(config));
//            System.out.println("saving ... " + pathname);
//
//            try {
//                FileUtils.writeByteArrayToFile(new File(pathname),
//                        gameData);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
}
