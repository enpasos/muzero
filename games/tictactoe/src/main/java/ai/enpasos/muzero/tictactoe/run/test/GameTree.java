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

import ai.enpasos.muzero.platform.agent.memorize.ZeroSumGame;
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
    List<DNode> forceableWinNodesPlayerA;
    List<DNode> forceableWinNodesPlayerB;
    List<DNode> terminatedGameNodes;
    List<DNode> unterminatedGameNodes;
    Set<ZeroSumGame> terminatedGames;
    DNode rootNode;

    public GameTree(MuZeroConfig config) {
        this.config = config;
        init();
    }

    private void init() {

        terminatedGameNodes = new ArrayList<>();
        unterminatedGameNodes = new ArrayList<>();

        rootNode = new DNode((ZeroSumGame) config.newGame());
        unterminatedGameNodes.add(rootNode);

        while (!unterminatedGameNodes.isEmpty()) {
            List<DNode> loopGameNodes = new ArrayList<>(unterminatedGameNodes);
            for (DNode node : loopGameNodes) {
                node.expand(unterminatedGameNodes, terminatedGameNodes);
            }
            log.info("unterminated games: " + unterminatedGameNodes.size() + ", terminated games: " + terminatedGameNodes.size());
        }


        terminatedGames = terminatedGameNodes.stream().map(DNode::getGame).collect(Collectors.toSet());


        propagateBestForceableValueBottomUp(OneOfTwoPlayer.PLAYER_A);
        propagateBestForceableValueBottomUp(OneOfTwoPlayer.PLAYER_B);

        Set<DNode> nodesWhereADecisionMatters = new HashSet<>();
        rootNode.findNodesWhereADecisionMatters(nodesWhereADecisionMatters);

        forceableWinNodesPlayerA = new ArrayList<>();
        rootNode.collectForceableWinNodes(OneOfTwoPlayer.PLAYER_A, forceableWinNodesPlayerA);

        forceableWinNodesPlayerB = new ArrayList<>();
        rootNode.collectForceableWinNodes(OneOfTwoPlayer.PLAYER_B, forceableWinNodesPlayerB);

    }

    private void propagateBestForceableValueBottomUp(@NotNull OneOfTwoPlayer player) {
        terminatedGameNodes.forEach(n -> n.setBestForceableValue(player, n.getValue(player)));
        rootNode.propagateBestForceableValueUp(player);
    }

}
