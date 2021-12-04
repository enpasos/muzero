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

package ai.enpasos.muzero.tictactoe.test;

import ai.enpasos.muzero.platform.agent.gamebuffer.ZeroSumGame;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GameTree {

    final MuZeroConfig config;
    List<DNode> forceableWinNodesPlayerA;
    List<DNode> forceableWinNodesPlayerB;
    List<DNode> terminatedGameNodes;
    List<DNode> unterminatedGameNodes;
    Set<ZeroSumGame> terminatedGameDTOs;
    DNode rootNode;

    public GameTree(MuZeroConfig config) {
        this.config = config;
        init();
    }
//    ReplayBuffer replayBuffer;

    private void init() {

        //  config = MuZeroConfig.getTicTacToeInstance();

//        ReplayBuffer replayBuffer = new ReplayBuffer(config);
//        replayBuffer.loadLatestState();
        // now let PlayerA and PlayerB play all possible moves


        terminatedGameNodes = new ArrayList<>();
        unterminatedGameNodes = new ArrayList<>();

        rootNode = new DNode((ZeroSumGame) config.newGame());
        unterminatedGameNodes.add(rootNode);

        int nBefore = 1;
        while (unterminatedGameNodes.size() > 0) {
            List<DNode> loopGameNodes = new ArrayList<>(unterminatedGameNodes);
            for (DNode node : loopGameNodes) {
                node.expand(unterminatedGameNodes, terminatedGameNodes);
            }
            System.out.println("unterminated games: " + unterminatedGameNodes.size() + ", terminated games: " + terminatedGameNodes.size());
        }


        terminatedGameDTOs = terminatedGameNodes.stream().map(DNode::getGame).collect(Collectors.toSet());


        propagateBestForceableValueBottomUp(OneOfTwoPlayer.PlayerA);
        propagateBestForceableValueBottomUp(OneOfTwoPlayer.PlayerB);

        Set<DNode> nodesWhereADecisionMatters = new HashSet<>();
        rootNode.findNodesWhereADecisionMatters(nodesWhereADecisionMatters);

        forceableWinNodesPlayerA = new ArrayList<>();
        rootNode.collectForceableWinNodes(OneOfTwoPlayer.PlayerA, forceableWinNodesPlayerA);

        forceableWinNodesPlayerB = new ArrayList<>();
        rootNode.collectForceableWinNodes(OneOfTwoPlayer.PlayerB, forceableWinNodesPlayerB);


        int i = 42;

    }

    private void propagateBestForceableValueBottomUp(@NotNull OneOfTwoPlayer player) {
        terminatedGameNodes.forEach(n -> n.setBestForceableValue(player, n.getValue(player)));
        rootNode.propagateBestForceableValueUp(player);
    }

}
