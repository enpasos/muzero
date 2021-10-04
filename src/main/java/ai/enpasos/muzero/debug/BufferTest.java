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
import ai.enpasos.muzero.environments.OneOfTwoPlayer;
import ai.enpasos.muzero.environments.go.GoGame;
import ai.enpasos.muzero.environments.tictactoe.TicTacToeGame;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.gamebuffer.GameDTO;
import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.gamebuffer.WinnerStatistics;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.gamebuffer.GameIO.getLatestBufferNo;

@Slf4j
public class BufferTest {

    public static void main(String[] args) {

        MuZeroConfig config = MuZeroConfig.getGoInstance(5);

        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        List<WinnerStatistics> winnerStatisticsList = new ArrayList<>();

        int cMax =  getLatestBufferNo(config);

        int start = 388000;

        for(int c = start; c <= cMax; c += 1000) {
            replayBuffer.loadState(c);


            //   log.info("total games: {}", replayBuffer.getBuffer().getData().size());

            List<Optional<OneOfTwoPlayer>> winnerList = replayBuffer.getBuffer().getGames().stream()
                    .map(g -> g.whoWonTheGame())
                    .collect(Collectors.toList());

            WinnerStatistics stats = WinnerStatistics.builder()
                    .allGames(winnerList.size())
                    .winPlayerACount(winnerList.stream().filter(o -> o.isPresent() && o.get() == OneOfTwoPlayer.PlayerA).count())
                    .winPlayerBCount(winnerList.stream().filter(o -> o.isPresent() && o.get() == OneOfTwoPlayer.PlayerB).count())
                    .drawCount(winnerList.stream().filter(o -> o.isEmpty()).count())
                    .build();

            //   System.out.println("A: " + stats.getWinPlayerACount() + ", B: " + stats.getWinPlayerBCount() +  ", draw: " + stats.getDrawCount());
            log.info("A: " + stats.getWinPlayerACount() + ", B: " + stats.getWinPlayerBCount() + ", draw: " + stats.getDrawCount());

            winnerStatisticsList.add(stats);
        }
        for (int i = 0; i <= (cMax - start) / 1000; i++) {
            int c = start + i * 1000;
            //   System.out.println("A: " + stats.getWinPlayerACount() + ", B: " + stats.getWinPlayerBCount() +  ", draw: " + stats.getDrawCount());
            System.out.println(c + ";" + winnerStatisticsList.get(i).getWinPlayerACount() + ";" + winnerStatisticsList.get(i).getAllGames() );
        }
//
//
//        Collection<GameDTO> collection = replayBuffer.getBuffer().getData();
//        GameDTO gameDTO = collection.iterator().next();
//        gameDTO.setRewards(List.of(42.0f));
//        replayBuffer.saveGame(new GoGame(config, gameDTO));
//
//        Set<Game> set = replayBuffer.getBuffer().getData().stream()
//                .map(dto -> {
//                    Game game = config.newGame();
//                    Objects.requireNonNull(game).setGameDTO(dto);
//                    return game;
//                }).collect(Collectors.toSet());
//        log.info("total games: {}", replayBuffer.getBuffer().getData().size());
//        log.info("unique games: {}", set.size());

    }

}
