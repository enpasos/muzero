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
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WinLooseStatistics {



    @Autowired
    private ReplayBuffer replayBuffer;

    public void winLooseStatisticsOnGamesInStoredBuffers(int start) {

        List<WinnerStatistics> winnerStatisticsList = new ArrayList<>();

        int cMax = replayBuffer.getLatestBufferNo();


        for (int c = start; c <= cMax; c += 1000) {
            replayBuffer.loadState(c);

            List<Optional<OneOfTwoPlayer>> winnerList = replayBuffer.getBuffer().getGames().stream()
                    .map(g -> ((ZeroSumGame) g).whoWonTheGame())
                    .collect(Collectors.toList());

            WinnerStatistics stats = WinnerStatistics.builder()
                    .allGames(winnerList.size())
                    .winPlayerACount(winnerList.stream().filter(o -> o.isPresent() && o.get() == OneOfTwoPlayer.PLAYER_A).count())
                    .winPlayerBCount(winnerList.stream().filter(o -> o.isPresent() && o.get() == OneOfTwoPlayer.PLAYER_B).count())
                    .drawCount(winnerList.stream().filter(Optional::isEmpty).count())
                    .build();

               log.info("A: " + stats.getWinPlayerACount() + ", B: " + stats.getWinPlayerBCount() + ", draw: " + stats.getDrawCount());

            winnerStatisticsList.add(stats);
        }
        for (int i = 0; i <= (cMax - start) / 1000; i++) {
            int c = start + i * 1000;
            log.info(c + ";" + winnerStatisticsList.get(i).getWinPlayerACount() + ";" + winnerStatisticsList.get(i).getAllGames());
        }

    }

}
