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
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.memorize.ZeroSumGame;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import ai.enpasos.muzero.tictactoe.config.TicTacToeGame;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TicTacToeTest2 {

    @Autowired
    MuZeroConfig config;

    @Autowired
    ReplayBuffer replayBuffer;

    @Autowired
    SelfPlay selfPlay;


    public boolean test() {

        config.setTemperatureRoot(0.0);


        replayBuffer.init();

        replayBuffer.loadLatestState();


        GameTree gameTree = new GameTree(config);


        Set<ZeroSumGame> bufferGameDTOs = replayBuffer.getBuffer().getGames().stream().map(d -> new TicTacToeGame(config, d.getGameDTO())).collect(Collectors.toSet());
        Set<ZeroSumGame> terminatedGameNotInBufferDTOs = gameTree.terminatedGameNodes.stream()
            .map(DNode::getGame)
            .filter(d -> !bufferGameDTOs.contains(d))
            .collect(Collectors.toSet());

        log.info("terminatedGames           : " + gameTree.terminatedGames.size());
        log.info("bufferGameDTOs               : " + bufferGameDTOs.size());
        log.info("terminatedGameNotInBufferDTOs: " + terminatedGameNotInBufferDTOs.size());

        Set<DNode> decisionNodes = new HashSet<>();
        gameTree.rootNode.collectDecisionNodes(decisionNodes);
        log.info("decisionNodes:                 " + decisionNodes.size());

        checkForProblematicNodes(decisionNodes);

        Set<DNode> nodesWhereADecisionMatters = new HashSet<>();
        gameTree.rootNode.findNodesWhereADecisionMatters(nodesWhereADecisionMatters);
        log.info("nodesWhereADecisionMatters:    " + nodesWhereADecisionMatters.size());

        List<DNode> wonByPlayerAGameNodes = gameTree.terminatedGameNodes.stream()
            .filter(g -> g.getGame().getEnvironment().hasPlayerWon(OneOfTwoPlayer.PLAYER_A))
            .collect(Collectors.toList());
        log.info("wonByPlayerAGameNodes: " + wonByPlayerAGameNodes.size());

        List<DNode> wonByPlayerBGameNodes = gameTree.terminatedGameNodes.stream()
            .filter(g -> g.getGame().getEnvironment().hasPlayerWon(OneOfTwoPlayer.PLAYER_B))
            .collect(Collectors.toList());

        log.info("wonByPlayerBGameNodes: " + wonByPlayerBGameNodes.size());

        checkForProblematicNodes(decisionNodes);

        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {

            Network network = new Network(config, model);
            try (NDManager nDManager = network.getNDManager().newSubManager()) {


                network.setHiddenStateNDManager(nDManager);
                network.initActionSpaceOnDevice(nDManager);


                List<DNode> gamesWithBadDecisions = new ArrayList<>();
                List<DNode> gamesWithBadDecisions2 = new ArrayList<>();


                notOptimal2(gameTree, network,   false, gamesWithBadDecisions, nodesWhereADecisionMatters);



                checkForProblematicNodes(decisionNodes);
                notOptimal2(gameTree, network,  true, gamesWithBadDecisions2, nodesWhereADecisionMatters);




                return gamesWithBadDecisions.isEmpty() &&
                    gamesWithBadDecisions2.isEmpty()  ;
            }

        }

    }

    public static void checkForProblematicNodes(Set<DNode> decisionNodes) {
        List<DNode> problematicNodes = decisionNodes.stream().filter(d -> !d.checkForActionListSize()).collect(Collectors.toList());
        log.info("problematicNodes: {}", problematicNodes.size());
        printActions(problematicNodes);
    }

    private static void printActions(List<DNode> nodes) {
        nodes.forEach(n ->
            log.info("{}", n.getGame().getGameDTO().getActions())
         );
    }


    private void notOptimal2(@NotNull GameTree gameTree, @NotNull Network network,  boolean withMCTS, @NotNull List<DNode> badDecisionGame,     Set<DNode> nodesWhereADecisionMatters) {
        gameTree.rootNode.clearAIDecisions();
        int count = 0;
        for (DNode n : nodesWhereADecisionMatters ) {
            log.info("{} of {}", count++, nodesWhereADecisionMatters.size());

            n.aiChosenChild = n.aiDecision(network, withMCTS, selfPlay );
            if( n.getGame().getEnvironment().getPlayerToMove() == OneOfTwoPlayer.PLAYER_A) {
                if (!n.aiChosenChild.bestForceableValuePlayerA.equals(n.bestForceableValuePlayerA)) {
                    badDecisionGame.add(n.aiChosenChild);
                }
            } else {
                if (!n.aiChosenChild.bestForceableValuePlayerB.equals(n.bestForceableValuePlayerB)) {
                    badDecisionGame.add(n.aiChosenChild);
                }
            }
        }
        log.info("Bad decisions with MCTS=" + withMCTS + ": " + badDecisionGame.size());
        printActions(badDecisionGame);


    }



    private @NotNull List<DNode> gamesLostByPlayer(@NotNull DNode rootNode, @NotNull Network network, boolean withMCTS, @NotNull OneOfTwoPlayer player) {
        rootNode.addAIDecisions(network, player, withMCTS, selfPlay);
        List<DNode> gamesLostByPlayerA = new ArrayList<>();
        rootNode.collectGamesLost(player, gamesLostByPlayerA);
        log.info("Games lost by " + player + " with MCTS=" + withMCTS + ": " + gamesLostByPlayerA.size());
        return gamesLostByPlayerA;
    }


}
