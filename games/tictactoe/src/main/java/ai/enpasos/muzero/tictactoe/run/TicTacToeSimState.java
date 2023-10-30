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

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.NetworkIOService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.LegalActionsDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.StateNodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.LegalActionsRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.StateNodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.run.GameProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeSimState {
    @Autowired
    MuZeroConfig config;

//    @Autowired
//    GameProvider gameProvider;
//
//
    @Autowired
    TimestepRepo timestepRepo;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    ModelService modelService;

    @Autowired
    LegalActionsRepo legalActionsRepo;


    @Autowired
    StateNodeRepo stateNodeRepo;

    @SuppressWarnings({"squid:S125", "CommentedOutCode"})
    public void run() {

        markSimilarityState();
        findStateNodes();
        markVisitedActions();
    }

    public void findStateNodes() {
        timestepRepo.deleteStateNodesRefs();
        stateNodeRepo.deleteAll();

        List<LegalActionsDO>  las =  legalActionsRepo.findAll();
        for (int i = 0; i < las.size(); i++) {
            long legalActionId = las.get(i).getId();

            LegalActionsDO legalActionsDO = legalActionsRepo.findLegalActionsDOWithTimeStepDOs(legalActionId);
            boolean[] legalActions = legalActionsDO.getLegalActions();
            Set<StateNodeDO> stateNodeSet = new HashSet<>();
            List<Pair<TimeStepDO, StateNodeDO>> pairList = new ArrayList<>();
            for (TimeStepDO ts : legalActionsDO.getTimeSteps()) {
                StateNodeDO stateNodeDO = StateNodeDO.builder()
                        .simState(ts.getSimState())
                        .legalActions(legalActions)
                        .build();
                stateNodeSet.add(stateNodeDO);
                pairList.add(new ImmutablePair<>(ts, stateNodeDO));
            }

              List<StateNodeDO> stateNodeList = stateNodeRepo.saveAll(stateNodeSet);



            for (StateNodeDO stateNodeDO : stateNodeSet) {
                List<TimeStepDO> timeStepDOs = pairList.stream().filter(p -> p.getRight().equals(stateNodeDO)).map(p -> p.getLeft()).collect(Collectors.toList());
                List<Long> timeStepIds = timeStepDOs.stream().map(ts -> ts.getId()).collect(Collectors.toList());
                log.debug("updateStateNodeIds({}, {})", stateNodeDO.getId(), timeStepIds);

                timestepRepo.updateStateNodeIds(stateNodeDO.getId(), timeStepIds);
            }

        }
    }

    public void markSimilarityState() {
        timestepRepo.clearSimState();

        try {
            modelService.loadLatestModelOrCreateIfNotExisting().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int pageSize =1000;

        Pair<List<Game>, Integer> gamesAndTotalPagesCounter = gameBuffer.getGamesByPage(0, pageSize);
        List<Game> games =  gamesAndTotalPagesCounter.getLeft();
        int totalPages = gamesAndTotalPagesCounter.getRight();
        updateSimStateForGames(games);

        for (int page = 1; page < totalPages; page++) {
            log.info("page: " + page + " of " + totalPages );
            gamesAndTotalPagesCounter = gameBuffer.getGamesByPage(page, pageSize);
            games =  gamesAndTotalPagesCounter.getLeft();
            updateSimStateForGames(games);
        }
    }

    public void markVisitedActions() {
        stateNodeRepo.deleteVisitedActions();

        List<StateNodeDO>  stateNodeDOs =  stateNodeRepo.findAll();
        int c = 0;
        for (int i = 0; i < stateNodeDOs.size(); i++) {
           long stateNodeDOId = stateNodeDOs.get(i).getId();
           log.info("{} of {}: update visited actions for stateNodeDO with id {}", c++, stateNodeDOs.size(), stateNodeDOId);
           StateNodeDO stateNodeDO = stateNodeRepo.findStateNodeDOWithTimeStepDOs(stateNodeDOId);
           // copy of stateNodeDO.getLegalActions()
            // boolean[] legalAndNotVisited =  Arrays.copyOf(stateNodeDO.getLegalActions(), stateNodeDO.getLegalActions().length);
           boolean[] visited = new boolean[stateNodeDO.getLegalActions().length];
           for (TimeStepDO ts : stateNodeDO.getTimeSteps()) {
               Integer a = ts.getAction();
               if (a != null) {
                   visited[a] = true;
               }
           }
           stateNodeDO.setVisitedActions(visited);
           stateNodeRepo.updateVisitedActions(stateNodeDOId, visited);
        }
    }

    public void updateSimStateForGames(List<Game> games) {
        int t = 0;
        while (games.size() > 0) {
            int tFinal = t;
            games.stream().forEach(g -> g.setObservationInputTime(tFinal));

            List<NetworkIO> networkOutputs = modelService.initialInference2(games).join();
            List<Pair<Long, float[]>> pairs = new ArrayList<>();
            for (int i = 0; i < games.size(); i++) {
                long id = games.get(i).getEpisodeDO().getTimeStep(t).getId();
                float[] similarityVector = networkOutputs.get(i).getSimilarityVector();
                timestepRepo.saveSimilarityVectors(id, similarityVector);
            }
            t++;
            int tFinal2 = t;
            games = games.stream().filter(g -> g.getEpisodeDO().getLastTime() >=  tFinal2).collect(Collectors.toList());
        }
    }

}
