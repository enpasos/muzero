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

import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.ZeroSumGame;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GameTree {

    final MuZeroConfig config;
    List<DNode> terminalGameNodes;
    List<DNode> nonExpandedGameNodes;

    final Set<DNode> nodesWhereADecisionMattersForPlayerA = new HashSet<>();
    Set<DNode> nodesWhereADecisionMattersForPlayerAOnOptimalPath = new HashSet<>();
    final Set<DNode> nodesWhereADecisionMattersForPlayerB = new HashSet<>();
    Set<DNode> nodesWhereADecisionMattersForPlayerBOnOptimalPath = new HashSet<>();
    DNode rootNode;

    public GameTree(MuZeroConfig config) {
        this.config = config;
        init();
    }

    private void init() {

        terminalGameNodes = new ArrayList<>();
        nonExpandedGameNodes = new ArrayList<>();

        // fully expand the game tree
        rootNode = new DNode((ZeroSumGame) config.newGame(true, true));
        nonExpandedGameNodes.add(rootNode);
        while (!nonExpandedGameNodes.isEmpty()) {
            List<DNode> loopGameNodes = new ArrayList<>(nonExpandedGameNodes);
            for (DNode node : loopGameNodes) {
                node.expand(nonExpandedGameNodes, terminalGameNodes);
            }
            log.info("not expanded game nodes: " + nonExpandedGameNodes.size() + ", terminal games: " + terminalGameNodes.size());
        }

        propagateBestForceableValueBottomUp(OneOfTwoPlayer.PLAYER_A);
        propagateBestForceableValueBottomUp(OneOfTwoPlayer.PLAYER_B);


        rootNode.findNodesWhereADecisionMatters(OneOfTwoPlayer.PLAYER_A, nodesWhereADecisionMattersForPlayerA);
        rootNode.findNodesWhereADecisionMatters(OneOfTwoPlayer.PLAYER_B, nodesWhereADecisionMattersForPlayerB);


        nodesWhereADecisionMattersForPlayerAOnOptimalPath
                = nodesWhereADecisionMattersForPlayerA.stream().filter(
                this::isOnOptimalPath
        ).collect(Collectors.toSet());

        nodesWhereADecisionMattersForPlayerBOnOptimalPath
                = nodesWhereADecisionMattersForPlayerB.stream().filter(
                this::isOnOptimalPath
        ).collect(Collectors.toSet());
    }

    public boolean isOnOptimalPath(DNode node) {
        return node.isOnOptimalPath(this.rootNode);
    }

    public List<DNode> badDecisionFinder(@NotNull GameTree gameTree, @NotNull OneOfTwoPlayer player, boolean withMCTS, Inference inference, int epoch, boolean onOptimalPathOnly) {

        Set<DNode> nodesWhereADecisionMatters;
        if (player == OneOfTwoPlayer.PLAYER_A) {

            nodesWhereADecisionMatters = onOptimalPathOnly ? gameTree.nodesWhereADecisionMattersForPlayerAOnOptimalPath
                    : gameTree.nodesWhereADecisionMattersForPlayerA;
        } else {
            nodesWhereADecisionMatters = onOptimalPathOnly ? gameTree.nodesWhereADecisionMattersForPlayerBOnOptimalPath
                    : gameTree.nodesWhereADecisionMattersForPlayerB;
        }

        List<DNode> nodeList = new ArrayList<>(nodesWhereADecisionMatters);
        List<Game> gameList = nodeList.stream().map(DNode::getGame).collect(Collectors.toList());


        int[] actions = inference.aiDecisionForGames(gameList, withMCTS, epoch);

        List<DNode> badDecisionNodes = new ArrayList<>();

        for (int i = 0; i < actions.length; i++) {
            int action = actions[i];
            DNode node = nodeList.get(i);
            if (!node.getBestForceableValue(player).equals(node.getChild(action).getBestForceableValue(player))) {
                badDecisionNodes.add(node.getChild(action));
            }
        }

        log.info("Games with bad decision by " + player + " with MCTS=" + withMCTS + ": " + badDecisionNodes.size());

        printActions(badDecisionNodes);

        return badDecisionNodes;


    }

    private void printActions(List<DNode> nodes) {
        nodes.forEach(n -> log.info("{}", n.getGame().getEpisodeDO().getActions()));
    }


    private void propagateBestForceableValueBottomUp(@NotNull OneOfTwoPlayer player) {
        terminalGameNodes.forEach(n -> n.setBestForceableValue(player, n.getValue(player)));
        rootNode.propagateBestForceableValueUp(player);
    }

}
