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

package ai.enpasos.muzero.tictactoe.run.test;

import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.c_model.Inference;
import ai.enpasos.muzero.platform.agent.c_model.Network;
import ai.enpasos.muzero.platform.agent.b_planning.SelfPlay;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ai.enpasos.muzero.platform.config.PlayTypeKey.PLAYOUT;

@Slf4j
@Component
public class TicTacToeTest {

    @Autowired
    MuZeroConfig config;

    @Autowired
    SelfPlay selfPlay;

    @Autowired
    Inference inference;

    public int findBadDecisions() {
        return findBadDecisions(-1, false);
    }

    /**
     * Returns the number of failures.
     *
     * @return number of failures
     */
    public int findBadDecisions(int epoch, boolean onOptimalPathOnly) {
        GameTree gameTree = prepareGameTree();
        return findBadDecisions(epoch, gameTree, onOptimalPathOnly);
    }

    public int findBadDecisions(int epoch, GameTree gameTree, boolean onOptimalPathOnly) {

                log.info("nodes where a decision matters for player X {}, for player O {}",
                    gameTree.nodesWhereADecisionMattersForPlayerA.size(),
                    gameTree.nodesWhereADecisionMattersForPlayerB.size()
                    );

                log.info("nodes where a decision on optimal path matters for player X {}, for player O {}",
                    gameTree.nodesWhereADecisionMattersForPlayerAOnOptimalPath.size(),
                    gameTree.nodesWhereADecisionMattersForPlayerBOnOptimalPath.size()
                );

//                List<DNode> gamesWithBadDecisionByPlayerA =
//                    gameTree.badDecisionFinder(gameTree, OneOfTwoPlayer.PLAYER_A, false, inference, epoch, onOptimalPathOnly);
//
//                List<DNode> gamesWithBadDecisionPlayerB =
//                    gameTree.badDecisionFinder(gameTree, OneOfTwoPlayer.PLAYER_B, false, inference, epoch, onOptimalPathOnly);

//
                List<DNode> gamesWithBadDecisionByPlayerA2 =
                    gameTree.badDecisionFinder(gameTree, OneOfTwoPlayer.PLAYER_A, true, inference, epoch, onOptimalPathOnly);

//                List<DNode> gamesWithBadDecisionByPlayerB2 =
//                     gameTree.badDecisionFinder(gameTree, OneOfTwoPlayer.PLAYER_B, true, inference, epoch, onOptimalPathOnly);
        return  0;

//                return gamesWithBadDecisionByPlayerA.size() +
//                    gamesWithBadDecisionPlayerB.size() +
//                    gamesWithBadDecisionByPlayerA2.size() +
//                    gamesWithBadDecisionByPlayerB2.size();

    }

    @NotNull
    public GameTree prepareGameTree() {
      //  config.setOutputDir("./memory/tictactoe-without-exploration/");

        config.setPlayTypeKey(PLAYOUT);

        GameTree gameTree = new GameTree(config);


        Set<DNode> nonTerminalNodes = new HashSet<>();
        gameTree.rootNode.collectNonTerminalNodes(nonTerminalNodes);
        log.info("nonTerminalNodes:                 " + nonTerminalNodes.size());

        log.info("nodes where a decision matters for Player A:    " + gameTree.nodesWhereADecisionMattersForPlayerA.size());
        log.info("nodes where a decision matters for Player B:    " + gameTree.nodesWhereADecisionMattersForPlayerB.size());
        return gameTree;
    }

}
