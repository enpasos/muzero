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

package ai.enpasos.muzero.gamebuffer;


import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.gamebuffer.modern.GameTree;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplayBufferDTO implements Serializable {

     final List<GameDTO> data = new ArrayList<>();
    private long counter;
    private int windowSize;

    transient GameTree gameTree;

    public ReplayBufferDTO(int windowSize) {
        this.windowSize = windowSize;
    }

    public void saveGame(@NotNull Game game, MuZeroConfig config) {
        while (data.size() >= windowSize) {
            GameDTO toberemoved = data.get(0);
            Game gameToberemoved = config.newGame();
            gameToberemoved.setGameDTO(toberemoved);
            getGameTree().removeGame(gameToberemoved);
            data.remove(0);
        }
        data.add(game.getGameDTO());
        getGameTree().addGame(game);
        counter++;
    }


    public void rebuildGameTree( MuZeroConfig config) {
        for (GameDTO gameDTO : data) {
            Game game  = config.newGame();
            game.setGameDTO(gameDTO);
            getGameTree().addGame(game);
        }
    }

    public void clear() {
        data.clear();
    }

    public GameTree getGameTree() {
        if (gameTree == null) gameTree = new GameTree();
        return gameTree;
    }
}
