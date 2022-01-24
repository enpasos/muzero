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

package ai.enpasos.muzero.go.selfcritical;

import ai.enpasos.muzero.go.config.GoGame;
import ai.enpasos.muzero.platform.agent.gamebuffer.*;
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
@SuppressWarnings("squid:S106")
public class SelfCritical {


    @Autowired
    private ReplayBuffer replayBuffer;

    @Autowired
    private SelfCriticalTrain train;

    public void run() {

        // assuming that the buffer is filled only by data produced by one (the latest) network
        replayBuffer.loadLatestState();


        int numOfGames = replayBuffer.getBuffer().getData().size();
        List<SelfCriticalLabeledFeature> rawFeatures = new ArrayList<>();

        SelfCriticalDataSet dataSet = new SelfCriticalDataSet();

        for(int g = 0; g < numOfGames; g++) {
            Game game = replayBuffer.getBuffer().getGames().get(g);
            for(int a = 0; a < game.getGameDTO().getActions().size(); a++) {
                rawFeatures.add(SelfCriticalLabeledFeature.builder()
                        .numberOfMovesPlayedSofar(a + 1)
                        .playerAWins(((GoGame)game).whoWonTheGame().get() == OneOfTwoPlayer.PLAYER_A)
                        .value(game.getGameDTO().getRootValues().get(a))
                        .toPlay((OneOfTwoPlayer) game.toPlay())
                    .build());
            }
        }

        dataSet.features = rawFeatures;

        dataSet.transformRawToNormalizedInput();

        try {
            train.run(dataSet);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
