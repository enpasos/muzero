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

package ai.enpasos.muzero.platform.debug;

import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.gamebuffer.WinnerStatistics;
import ai.enpasos.muzero.platform.agent.gamebuffer.ZeroSumGame;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.agent.gamebuffer.GameIO.getLatestBufferNo;

@Slf4j
public class WinLooseStatistics {

    public static void winLooseStatisticsOnGamesInStoredBuffers(MuZeroConfig config, int start) {
        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        List<WinnerStatistics> winnerStatisticsList = new ArrayList<>();

        int cMax = getLatestBufferNo(config);


        for (int c = start; c <= cMax; c += 1000) {
            replayBuffer.loadState(c);


            //   log.info("total games: {}", replayBuffer.getBuffer().getData().size());

            List<Optional<OneOfTwoPlayer>> winnerList = replayBuffer.getBuffer().getGames().stream()
                    .map(g -> ((ZeroSumGame) g).whoWonTheGame())
                    .collect(Collectors.toList());

            WinnerStatistics stats = WinnerStatistics.builder()
                    .allGames(winnerList.size())
                    .winPlayerACount(winnerList.stream().filter(o -> o.isPresent() && o.get() == OneOfTwoPlayer.PlayerA).count())
                    .winPlayerBCount(winnerList.stream().filter(o -> o.isPresent() && o.get() == OneOfTwoPlayer.PlayerB).count())
                    .drawCount(winnerList.stream().filter(Optional::isEmpty).count())
                    .build();

            //   System.out.println("A: " + stats.getWinPlayerACount() + ", B: " + stats.getWinPlayerBCount() +  ", draw: " + stats.getDrawCount());
            log.info("A: " + stats.getWinPlayerACount() + ", B: " + stats.getWinPlayerBCount() + ", draw: " + stats.getDrawCount());

            winnerStatisticsList.add(stats);
        }
        for (int i = 0; i <= (cMax - start) / 1000; i++) {
            int c = start + i * 1000;
            //   System.out.println("A: " + stats.getWinPlayerACount() + ", B: " + stats.getWinPlayerBCount() +  ", draw: " + stats.getDrawCount());
            System.out.println(c + ";" + winnerStatisticsList.get(i).getWinPlayerACount() + ";" + winnerStatisticsList.get(i).getAllGames());
        }

    }

}
