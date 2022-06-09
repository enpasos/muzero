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
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("squid:S2065")
public class ReplayBufferDTO {

    private List<GameDTO> initialGameDTOList = new ArrayList<>();

    public List<GameDTO> getDTOListFromGames() {
        return games.stream().map(game -> game.getGameDTO()).collect(Collectors.toList());
    }


    transient List<Game> games = new ArrayList<>();
    private String gameClassName;
    private long counter;
    private int windowSize;


    public ReplayBufferDTO(int windowSize, String gameClassName) {
        this.windowSize = windowSize;
        this.gameClassName = gameClassName;
    }

    public ReplayBufferDTO copyEnvelope() {
        ReplayBufferDTO copy = new ReplayBufferDTO();
        copy.windowSize = this.windowSize;
        copy.counter = this.counter;
        copy.gameClassName = this.gameClassName;
        return copy;
    }

    public boolean isBufferFilled() {
        return games.size() >= windowSize;
    }


    public void removeGame(Game game) {
        games.remove(game);
        //data.remove(game.getGameDTO());
    }

    public void saveGame(@NotNull Game game, MuZeroConfig config) {
        removeGame(game); // do not keep old copies with the same action history
        while (isBufferFilled()) {
            GameDTO toBeRemoved = games.get(0).getGameDTO();
            Game gameToBeRemoved = config.newGame();
            gameToBeRemoved.setGameDTO(toBeRemoved);
            games.remove(gameToBeRemoved);
            // data.remove(0);
        }
        //   data.add(game.getGameDTO());
        if (!game.terminal()) {
            game.replayToPosition(game.actionHistory().getActionIndexList().size());
        }
        games.add(game);
        counter++;
        game.getGameDTO().setCount(counter);

        List<Game> toRemove = games.stream()
            .filter(g -> counter - g.getGameDTO().getCount() > config.getMaxGameLiveTime())
            .collect(Collectors.toList());
        toRemove.forEach(this::removeGame);


    }

    public void rebuildGames(MuZeroConfig config) {
        games = new ArrayList<>();
        for (GameDTO gameDTO : getDTOListFromGames()) {
            Game game = config.newGame();
            game.setGameDTO(gameDTO);
            if (!game.terminal()) {
                game.replayToPosition(game.actionHistory().getActionIndexList().size());
            }
            games.add(game);
        }
    }


//    public void clear() {
//        data.clear();
//    }


    public ReplayBufferProto proto() {
        ReplayBufferProto.Builder bufferBuilder = ReplayBufferProto.newBuilder()
            .setVersion(1)
            .setCounter((int) getCounter())
            .setWindowSize(getWindowSize())
            .setGameClassName(getGameClassName());

        games.forEach(game -> bufferBuilder.addGameProtos(game.getGameDTO().proto()));

        return bufferBuilder.build();
    }

    public void deproto(ReplayBufferProto proto) {

        this.setGameClassName(proto.getGameClassName());
        this.setCounter(proto.getCounter());
        this.setWindowSize(proto.getWindowSize());

        proto.getGameProtosList().forEach(p -> {
            GameDTO gameDTO = new GameDTO();
            gameDTO.deproto(p);
            this.getInitialGameDTOList().add(gameDTO);
        });
    }


}
