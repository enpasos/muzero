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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BufferTest {

    public static void main(String[] args) {

        MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();

        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        replayBuffer.loadLatestState();
        System.out.println("total games: " + replayBuffer.getBuffer().getData().size());
        //  replayBuffer.saveState();
        //  replayBuffer.loadLatestState();
        //  System.out.println("total games after save and reload: " + replayBuffer.getBuffer().getData().size() );


        Collection<GameDTO> collection = replayBuffer.getBuffer().getData().values();
        GameDTO gameDTO = collection.iterator().next();
        gameDTO.setRewards(List.of(42.0f));
        replayBuffer.saveGame(new TicTacToeGame(config, gameDTO));


        GameDTO[] a = collection.toArray(collection.toArray(new GameDTO[0]));
        GameDTO first = a[0];
        GameDTO last = a[a.length - 1];

        Set<Game> set = new HashSet<>();

        List<Game> gamesList = replayBuffer.getBuffer().getData().values().stream()
                .map(dto -> {
                    Game game = config.newGame();
                    game.setGameDTO(dto);
                    return game;
                })
                .collect(Collectors.toList());


        set.addAll(gamesList);
        System.out.println("total games: " + replayBuffer.getBuffer().getData().size());
        System.out.println("unique games: " + set.size());

    }

}
