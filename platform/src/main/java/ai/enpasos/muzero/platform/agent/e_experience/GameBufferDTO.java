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

package ai.enpasos.muzero.platform.agent.e_experience;


import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.memory.protobuf.GameBufferProto;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("squid:S2065")
public class GameBufferDTO {

    public static final double BUFFER_IO_VERSION = 1.0;
    transient List<Game> games = new ArrayList<>();
    transient MuZeroConfig config;
   // private List<GameDTO> initialGameDTOList = new ArrayList<>();
    private List<EpisodeDO> initialEpisodeDOList = new ArrayList<>();


    private String gameClassName;
    private long counter;

    public GameBufferDTO(MuZeroConfig config) {
        this.gameClassName = config.getGameClassName();
        this.config = config;

    }

    public boolean isBufferFilled() {
        return games.size() >= getWindowSize();
    }

    public int getWindowSize() {
        return config.getWindowSize();
    }

    public void removeGame(Game game) {
        games.remove(game);
    }

    public boolean addGameAndRemoveOldGameIfNecessary(@NotNull Game game, boolean atBeginning) {
        while (isBufferFilled()) {
            games.remove(0);
        }
        addGame( game,  atBeginning);
        return !isBufferFilled();
    }

    public void addGame(@NotNull Game game, boolean atBeginning) {
        if (!isBufferFilled()) {
            if (atBeginning) {
                games.add(0, game);
            } else {
                games.add(game);
            }
            if (game.getEpisodeDO().getCount() == 0 ) {
                counter++;
                game.getEpisodeDO().setCount(counter);
            } else {
                counter = Math.max(counter, game.getEpisodeDO().getCount());
            }
        }
    }


    public void rebuildGames(MuZeroConfig config) {
        log.info("rebuildGames");
        games = new ArrayList<>();
        for (EpisodeDO episodeDO : getInitialEpisodeDOList()) {
            Game game = config.newGame(false,false);
            game.setEpisodeDO(episodeDO);
            games.add(game);
        }
        getInitialEpisodeDOList().clear();
    }



    public boolean deepEquals(GameBufferDTO dtoNew) {
        // implement a deep equals
        boolean base = this.config.equals(dtoNew.config)
                && this.counter == dtoNew.counter
                && this.gameClassName.equals(dtoNew.gameClassName)
                && this.games.size() == dtoNew.games.size();

        if (!base) return false;

        for (int i = 0; i < this.games.size(); i++) {
            if (!this.games.get(i).deepEquals(dtoNew.getGames().get(i))) return false;
        }
        return true;
    }
}
