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

package ai.enpasos.muzero.debug;

import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.environments.tictactoe.TicTacToeGame;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.gamebuffer.GameDTO;
import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class BufferTest {

    public static void main(String[] args) {

        MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();

        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        replayBuffer.loadLatestState();
        log.info("total games: {}", replayBuffer.getBuffer().getData().size());


        Collection<GameDTO> collection = replayBuffer.getBuffer().getData();
        GameDTO gameDTO = collection.iterator().next();
        gameDTO.setRewards(List.of(42.0f));
        replayBuffer.saveGame(new TicTacToeGame(config, gameDTO));

        Set<Game> set = replayBuffer.getBuffer().getData().stream()
                .map(dto -> {
                    Game game = config.newGame();
                    Objects.requireNonNull(game).setGameDTO(dto);
                    return game;
                }).collect(Collectors.toSet());
        log.info("total games: {}", replayBuffer.getBuffer().getData().size());
        log.info("unique games: {}", set.size());

    }

}
