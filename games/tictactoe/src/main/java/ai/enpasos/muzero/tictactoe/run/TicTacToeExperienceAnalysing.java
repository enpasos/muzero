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

package ai.enpasos.muzero.tictactoe.run;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.NetworkIOService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.run.GameProvider;
import ai.enpasos.muzero.tictactoe.config.TicTacToeGame;
import ai.enpasos.muzero.tictactoe.run.exploitability.TicTacToeTestValueMetric;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.convertEpisodeDOsToGames;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeExperienceAnalysing {
    @Autowired
    MuZeroConfig config;

    @Autowired
    EpisodeRepo episodeRepo;

    @Autowired
    TicTacToeTestValueMetric ticTacToeTestValueMetric;



    @SuppressWarnings({"squid:S125", "CommentedOutCode"})
    public void run() {

        // a list of all possible terminal graph (not tree) nodes
        List<Game> games2 = ticTacToeTestValueMetric.getGamesForLeafNodes( );

        Set<String> endStatesPossible = games2.stream().map(g -> ((TicTacToeGame)g).getStateString()).collect(Collectors.toSet());


        Set<EpisodeDO> episodeDOSet = new HashSet<>();

        long maxCount = episodeRepo.getMaxCount();
        int increase = 10000;
        for (int c = 0; c <= maxCount; c += increase) {
            List<Long> ids = episodeRepo.findEpisodeIdsInCountInterval(c, c + increase - 1);
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
            episodeDOSet.addAll(episodeDOList);
        }

        List<Game> games = convertEpisodeDOsToGames(new ArrayList<>(episodeDOSet), config);


        Set<String> endStatesInMemory = games.stream().map(g -> ((TicTacToeGame)g).getStateString()).collect(Collectors.toSet());

        Set<String> endStatesPossibleButNotInMemory = new HashSet<>(endStatesPossible);
        endStatesPossibleButNotInMemory.removeAll(endStatesInMemory);

        System.out.println("endStatesPossibleButNotInMemory: " + endStatesPossibleButNotInMemory);

        int i = 42;



    }

}
