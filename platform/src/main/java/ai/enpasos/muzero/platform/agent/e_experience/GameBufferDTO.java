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
    private List<GameDTO> initialGameDTOList = new ArrayList<>();


    private String gameClassName;
    private long counter;

    public GameBufferDTO(MuZeroConfig config) {
        this.gameClassName = config.getGameClassName();
        this.config = config;

    }

    public static GameBufferDTO deproto(GameBufferProto proto, MuZeroConfig config) {
        GameBufferDTO dto = new GameBufferDTO(config);
        dto.setCounter(proto.getCounter());


        proto.getGameProtosList().forEach(p -> {
            GameDTO gameDTO = new GameDTO();
            gameDTO.deproto(p);
            dto.getInitialGameDTOList().add(gameDTO);
        });
        return dto;
    }

    public List<GameDTO> getDTOListFromGames() {
        return games.stream().map(Game::getGameDTO).collect(Collectors.toList());
    }

    public int getNumOfDifferentGames() {
        return games.stream().map(Game::getGameDTO).collect(Collectors.toSet()).size();
    }

    public GameBufferDTO copyEnvelope() {
        GameBufferDTO copy = new GameBufferDTO();

        copy.counter = this.counter;
        copy.gameClassName = this.gameClassName;
        return copy;
    }

    public void sortGamesByLastValueError() {
        getGames().sort(
            (Game g1, Game g2) -> Float.compare(g2.getError(), g1.getError()));
    }

    public void keepOnlyTheLatestGames(int n) {
        games = games.subList(Math.max(games.size() - n, 0), games.size());
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
            if (game.getGameDTO().getCount() == 0 ) {
                counter++;
                game.getGameDTO().setCount(counter);
            } else {
                counter = Math.max(counter, game.getGameDTO().getCount());
            }
        }
    }


    public void rebuildGames(MuZeroConfig config) {
        log.info("rebuildGames");
        games = new ArrayList<>();
        for (GameDTO gameDTO : getInitialGameDTOList()) {
            Game game = config.newGame(false,false);
            game.setGameDTO(gameDTO);
            games.add(game);
        }
        getInitialGameDTOList().clear();
    }

    public GameBufferProto proto() {
        GameBufferProto.Builder bufferBuilder = GameBufferProto.newBuilder()
            .setVersion(1)
            .setCounter((int) getCounter())

            .setGameClassName(getGameClassName());

        games.forEach(game -> bufferBuilder.addGameProtos(game.getGameDTO().proto()));

        return bufferBuilder.build();
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
