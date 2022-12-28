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

package ai.enpasos.muzero.platform.agent.memorize;


import ai.enpasos.muzero.platform.agent.memory.protobuf.ReplayBufferProto;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("squid:S2065")
public class ReplayBufferDTO {

    public static final double BUFFER_IO_VERSION = 1.0;
    transient List<Game> games = new ArrayList<>();
    transient MuZeroConfig config;
    private List<GameDTO> initialGameDTOList = new ArrayList<>();


    private String gameClassName;
    private long counter;

    public ReplayBufferDTO(MuZeroConfig config) {
        this.gameClassName = config.getGameClassName();
        this.config = config;

    }

    public static ReplayBufferDTO deproto(ReplayBufferProto proto, MuZeroConfig config) {
        ReplayBufferDTO dto = new ReplayBufferDTO(config);
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

    public ReplayBufferDTO copyEnvelope() {
        ReplayBufferDTO copy = new ReplayBufferDTO();

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
        return config.getWindowSize(this.getCounter());
    }

    public void removeGame(Game game) {
        games.remove(game);
    }

    public boolean addGameAndRemoveOldGameIfNecessary(@NotNull Game game, boolean atBeginning) {
        while (isBufferFilled()) {
            games.remove(0);
        }
        if (!game.terminal()) {
            game.replayToPosition(game.actionHistory().getActionIndexList().size());
        }
        if (atBeginning) {
            games.add(0, game);
        } else {
            games.add(game);
        }
        counter++;
        game.getGameDTO().setCount(counter);
        return !isBufferFilled();
    }
    public void addGame(@NotNull Game game, boolean atBeginning) {

        if (atBeginning) {
            games.add(0, game);
        } else {
            games.add(game);
        }
        counter++;
        game.getGameDTO().setCount(counter);

    }


    public void rebuildGames(MuZeroConfig config, boolean withReplay) {
        log.info("rebuildGames");
        games = new ArrayList<>();
        for (GameDTO gameDTO : getInitialGameDTOList()) {
            Game game = config.newGame();
            game.setGameDTO(gameDTO);
            if (!game.terminal() && withReplay) {
                game.replayToPosition(game.actionHistory().getActionIndexList().size());
            }
            games.add(game);
        }
        getInitialGameDTOList().clear();
    }

    public ReplayBufferProto proto() {
        ReplayBufferProto.Builder bufferBuilder = ReplayBufferProto.newBuilder()
            .setVersion(1)
            .setCounter((int) getCounter())

            .setGameClassName(getGameClassName());

        games.forEach(game -> bufferBuilder.addGameProtos(game.getGameDTO().proto()));

        return bufferBuilder.build();
    }


}
