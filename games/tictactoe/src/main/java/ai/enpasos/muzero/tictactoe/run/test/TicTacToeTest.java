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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.config.PlayTypeKey.PLAYOUT;

@Slf4j
@Component
public class TicTacToeTest {

    @Autowired
    MuZeroConfig config;

    @Autowired
    ReplayBuffer replayBuffer;

    @Autowired
    SelfPlay selfPlay;


    public boolean test() {

      //  config.setTemperatureRoot(  0.0);
        config.setPlayTypeKey(PLAYOUT);


        replayBuffer.init();

        replayBuffer.loadLatestState();
        // now let PlayerA and PlayerB play all possible moves


        GameTree gameTree = new GameTree(config);


        Set<ZeroSumGame> bufferGameDTOs = replayBuffer.getBuffer().getGames().stream().map(d -> new TicTacToeGame(config, d.getGameDTO())).collect(Collectors.toSet());
        Set<ZeroSumGame> terminatedGameNotInBufferDTOs = gameTree.terminatedGameNodes.stream()
            .map(DNode::getGame)
            .filter(d -> !bufferGameDTOs.contains(d))
            .collect(Collectors.toSet());

        log.info("terminatedGameDTOs           : " + gameTree.terminatedGames.size());
        log.info("bufferGameDTOs               : " + bufferGameDTOs.size());
        log.info("terminatedGameNotInBufferDTOs: " + terminatedGameNotInBufferDTOs.size());

        Set<DNode> decisionNodes = new HashSet<>();
        gameTree.rootNode.collectDecisionNodes(decisionNodes);
        log.info("decisionNodes:                 " + decisionNodes.size());

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


        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {

            Network network = new Network(config, model);
            try (NDManager nDManager = network.getNDManager().newSubManager()) {


                network.setHiddenStateNDManager(nDManager);
                network.initActionSpaceOnDevice(nDManager);


                List<DNode> gamesLostByPlayerA = new ArrayList<>();
                notOptimal(gameTree, network, OneOfTwoPlayer.PLAYER_A, false, gamesLostByPlayerA);

                List<DNode> gamesLostByPlayerB = new ArrayList<>();
                notOptimal(gameTree, network, OneOfTwoPlayer.PLAYER_B, false, gamesLostByPlayerB);


                List<DNode> gamesLostByPlayerA2 = new ArrayList<>();
                notOptimal(gameTree, network, OneOfTwoPlayer.PLAYER_A, true, gamesLostByPlayerA2);

                List<DNode> gamesLostByPlayerB2 = new ArrayList<>();
                notOptimal(gameTree, network, OneOfTwoPlayer.PLAYER_B, true, gamesLostByPlayerB2);


                return gamesLostByPlayerA.isEmpty() &&
                    gamesLostByPlayerB.isEmpty() &&
                    gamesLostByPlayerA2.isEmpty() &&
                    gamesLostByPlayerB2.isEmpty();
            }

        }

    }

    private void printActions(List<DNode> nodes) {
        nodes.forEach(n -> log.info("{}", n.getGame().getGameDTO().getActions()));
    }

    private void notOptimal(@NotNull GameTree gameTree, @NotNull Network network, @NotNull OneOfTwoPlayer player, boolean withMCTS, @NotNull List<DNode> gamesLostByPlayer) {
        gameTree.rootNode.clearAIDecisions();
        gameTree.rootNode.addAIDecisions(network, player, withMCTS, selfPlay);


        gameTree.rootNode.collectGamesLost(player, gamesLostByPlayer);
        log.info("Games lost by " + player + " with MCTS=" + withMCTS + ": " + gamesLostByPlayer.size());

        printActions(gamesLostByPlayer);


    }


    private @NotNull List<DNode> gamesLostByPlayer(@NotNull DNode rootNode, @NotNull Network network, boolean withMCTS, @NotNull OneOfTwoPlayer player) {
        rootNode.addAIDecisions(network, player, withMCTS, selfPlay);
        List<DNode> gamesLostByPlayerA = new ArrayList<>();
        rootNode.collectGamesLost(player, gamesLostByPlayerA);
        log.info("Games lost by " + player + " with MCTS=" + withMCTS + ": " + gamesLostByPlayerA.size());
        return gamesLostByPlayerA;
    }


}
