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

package ai.enpasos.muzero.platform.agent.gamebuffer;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplayBufferDTO implements Serializable {

    final List<GameDTO> data = new ArrayList<>();
    //  transient GameTree gameTree;
    transient List<Game> games = new ArrayList<>();
    private long counter;
    private int windowSize;

    public ReplayBufferDTO(int windowSize) {
        this.windowSize = windowSize;
    }

    public boolean isBufferFilled() {
        return data.size() >= windowSize;
    }

    public void saveGame(@NotNull Game game, MuZeroConfig config) {
        //  System.out.println(game.actionHistory().getActionIndexList());
        while (isBufferFilled()) {
            GameDTO toberemoved = data.get(0);
            Game gameToberemoved = config.newGame();
            gameToberemoved.setGameDTO(toberemoved);
            games.remove(gameToberemoved);
            data.remove(0);
        }
        data.add(game.getGameDTO());
        if (!game.terminal()) {
            game.replayToPosition(game.actionHistory().getActionIndexList().size());
        }
        games.add(game);
        //    getGameTree().addGame(game);
        counter++;
        // System.out.println(game.actionHistory().getActionIndexList());
    }


    public void clear() {
        data.clear();
    }

}
