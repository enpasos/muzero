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

package ai.enpasos.muzero.platform.agent.b_planning;

import ai.enpasos.muzero.platform.agent.b_planning.service.PlayService;
import ai.enpasos.muzero.platform.agent.d_experience.Game;
import ai.enpasos.muzero.platform.agent.d_experience.GameBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static ai.enpasos.muzero.platform.agent.b_planning.GumbelFunctions.add;
import static ai.enpasos.muzero.platform.agent.b_planning.GumbelFunctions.sigmas;
import static ai.enpasos.muzero.platform.common.Functions.softmax;


@Slf4j
@Component
@Data
public class SelfPlay {

    @Autowired
    PlayService multiGameStarter;

    @Autowired
    MuZeroConfig config;


    @Autowired
    GameBuffer gameBuffer;

    private List<Game> gameList;
    private List<Game> gamesDoneList;

    private static void clean(@NotNull Node node) {
        if (node.getHiddenState() != null) {
            node.getHiddenState().close();
            node.setHiddenState(null);
        }
        node.getChildren().forEach(SelfPlay::clean);
    }

    public static void storeSearchStatistics(Game game, @NotNull Node root, boolean justPriorValues, MuZeroConfig config, Action selectedAction, MinMaxStats minMaxStats) {

        game.getGameDTO().getRootValueTargets().add((float) root.getImprovedValue());

        float[] policyTarget = new float[config.getActionSpaceSize()];
        if (justPriorValues) {
            root.getChildren().forEach(node -> policyTarget[node.getAction().getIndex()] = (float) node.getPrior());
        } else if (root.getChildren().size() == 1) {
            policyTarget[selectedAction.getIndex()] = 1f;
        } else {

            double[] logits = root.getChildren().stream().mapToDouble(node -> node.getGumbelAction().getLogit()).toArray();

            double[] completedQsNormalized = root.getCompletedQValuesNormalized(minMaxStats);

            int[] actions = root.getChildren().stream().mapToInt(node -> node.getAction().getIndex()).toArray();

            int maxActionVisitCount = root.getChildren().stream().mapToInt(Node::getVisitCount).max().getAsInt();
            double[] raw = add(logits, sigmas(completedQsNormalized, maxActionVisitCount, config.getCVisit(), config.getCScale()));

            double[] improvedPolicy = softmax(raw);


            for (int i = 0; i < raw.length; i++) {
                int action = actions[i];
                double v = improvedPolicy[i];
                policyTarget[action] = (float) v;
            }
        }
        game.getGameDTO().getPolicyTargets().add(policyTarget);
    }





    private void hybridConfiguration(int gameLength) {
        gameList.stream().forEach(game -> {
            game.getGameDTO().setHybrid(true);
            if (game.getGameDTO().getTHybrid() == -1) {
                game.getGameDTO().setTHybrid(ThreadLocalRandom.current().nextInt(0, gameLength + 1));
            }
        });
    }



    public void playMultipleEpisodes2(boolean render, boolean fastRuleLearning, boolean justInitialInferencePolicy) {
        List<Game> games = new ArrayList<>();
        List<Game>  gamesToReanalyse = null;
        if (config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            gamesToReanalyse = gameBuffer.getGamesToReanalyse();
            if (gamesToReanalyse.size() > 0) {
                int i = 42;
            }
        }
        //for (int i = 0; i < config.getNumEpisodes(); i++) {
         //   List<Game> gamesPart = new ArrayList<>();
            if (config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
//                gamesPart = replayGames(network, gamesToReanalyse);
                games = multiGameStarter.reanalyseGames( config.getNumParallelGamesPlayed(),
                    PlayParameters.builder()
                        .render(render)
                        .fastRulesLearning(fastRuleLearning)
                        .build(),
                    gamesToReanalyse);
            } else {
              //  gamesPart = playGame(network, render, fastRuleLearning, justInitialInferencePolicy);
                games = multiGameStarter.playNewGames( config.getNumParallelGamesPlayed(),
                    PlayParameters.builder()
                        .render(render)
                        .fastRulesLearning(fastRuleLearning)
                        .build());
            }

            log.info("Played {} games parallel", games.size());
           // games.addAll(gamesPart);
      //  }
        gameBuffer.addGames2( games, false);
    }



}


