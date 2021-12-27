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


import ai.enpasos.muzero.platform.agent.gamebuffer.protobuf.ReplayBufferProto;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("squid:S2065")
public class ReplayBufferDTO {

    final List<GameDTO> data = new ArrayList<>();
    transient List<Game> games = new ArrayList<>();
    private String gameClassName;
    private long counter;
    private int windowSize;


    public ReplayBufferDTO(int windowSize, String gameClassName) {
        this.windowSize = windowSize;
        this.gameClassName = gameClassName;
    }

    public boolean isBufferFilled() {
        return data.size() >= windowSize;
    }

    public void saveGame(@NotNull Game game, MuZeroConfig config) {
        while (isBufferFilled()) {
            GameDTO toBeRemoved = data.get(0);
            Game gameToBeRemoved = config.newGame();
            gameToBeRemoved.setGameDTO(toBeRemoved);
            games.remove(gameToBeRemoved);
            data.remove(0);
        }
        data.add(game.getGameDTO());
        if (!game.terminal()) {
            game.replayToPosition(game.actionHistory().getActionIndexList().size());
        }
        games.add(game);
        counter++;
    }


    public void clear() {
        data.clear();
    }


    public ReplayBufferProto proto() {
         ReplayBufferProto.Builder bufferBuilder = ReplayBufferProto.newBuilder()
                .setVersion(1)
                .setCounter((int)getCounter())
                .setWindowSize(getWindowSize())
                .setGameClassName(getGameClassName());

        getData().stream().forEach( gameDTO -> bufferBuilder.addGameProtos(gameDTO.proto()));

        return bufferBuilder.build();
    }

    public void deproto(ReplayBufferProto proto) {

        this.setGameClassName(proto.getGameClassName());
        this.setCounter(proto.getCounter());
        this.setWindowSize(proto.getWindowSize());

        proto.getGameProtosList().stream().forEach(p -> {
            GameDTO gameDTO = new GameDTO();
            gameDTO.deproto(p);
            this.getData().add(gameDTO);
        });
    }
}
