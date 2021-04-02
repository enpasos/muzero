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

package ai.enpasos.muzero.debug;

import ai.djl.Model;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.environments.OneOfTwoPlayer;
import ai.enpasos.muzero.environments.tictactoe.TicTacToeGame;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.network.Network;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TicTacToeTest {


    public static void main(String[] args) {


        MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();

        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        replayBuffer.loadLatestState();
        // now let PlayerA and PlayerB play all possible moves


        GameTree gameTree = new GameTree(config);


        Set<Game> bufferGameDTOs = replayBuffer.getBuffer().getData().values().stream().map(d -> new TicTacToeGame(config, d)).collect(Collectors.toSet());
        Set<Game> terminatedGameNotInBufferDTOs = gameTree.terminatedGameNodes.stream()
                .map(n -> n.getGame())
                .filter(d -> !bufferGameDTOs.contains(d))
                .collect(Collectors.toSet());


        //   terminatedGameNotInBufferDTOs.stream().forEach(g -> System.out.println(g.actionHistory().getActionIndexList()));

//        if (terminatedGameNotInBufferDTOs.iterator().hasNext()) {
//            Game game = terminatedGameNotInBufferDTOs.iterator().next();
//            renderGame(config, game);
//            boolean contained = replayBuffer.getBuffer().getData().values().contains(game.getGameDTO());
//            System.out.println("game is contained in buffer: " + contained);
//        }


        Set<GameState> gameStateSet = gameTree.terminatedGameDTOs.stream()
                .map(g -> new GameState(g))
                .collect(Collectors.toSet());

        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {
            Network network = new Network(config, model);
            gameStateSet.stream().forEach(gs -> {
                DNode n = new DNode(gs.getGame());
                n.aiChosenChild = n.aiDecision(network, false);
                float reward = n.getGame().getGameDTO().getRewards().get(n.getGame().getGameDTO().getRewards().size() - 1);
                System.out.println("aiValue: " + n.aiValue + ", value:" + (-reward));
            });
        }


        System.out.println("terminatedGameDTOs           : " + gameTree.terminatedGameDTOs.size());
        System.out.println("bufferGameDTOs               : " + bufferGameDTOs.size());
        System.out.println("terminatedGameNotInBufferDTOs: " + terminatedGameNotInBufferDTOs.size());

        Set<DNode> decisionNodes = new HashSet<>();
        gameTree.rootNode.collectDecisionNodes(decisionNodes);
        System.out.println("decisionNodes:                 " + decisionNodes.size());

        Set<DNode> nodesWhereADecisionMatters = new HashSet<>();
        gameTree.rootNode.findNodesWhereADecisionMatters(nodesWhereADecisionMatters);
        System.out.println("nodesWhereADecisionMatters:    " + nodesWhereADecisionMatters.size());


        List<DNode> wonByPlayerAGameNodes = gameTree.terminatedGameNodes.stream()
                .filter(g -> g.getGame().getEnvironment().hasPlayerWon(OneOfTwoPlayer.PlayerA))
                .collect(Collectors.toList());
        System.out.println("wonByPlayerAGameNodes: " + wonByPlayerAGameNodes.size());

        List<DNode> wonByPlayerBGameNodes = gameTree.terminatedGameNodes.stream()
                .filter(g -> g.getGame().getEnvironment().hasPlayerWon(OneOfTwoPlayer.PlayerB))
                .collect(Collectors.toList());

        System.out.println("wonByPlayerBGameNodes: " + wonByPlayerBGameNodes.size());

        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {
            Path modelPath = Paths.get("./");

            Network network = new Network(config, model); //, modelPath);

            //  nodesWithBadDecisions(nodesWhereADecisionMatters, network, true);


//            gameTree.forceableWinNodesPlayerA.stream().forEach(
//                    n -> n.addAIDecisions(network, OneOfTwoPlayer.PlayerA, false)
//            );

            int i = 42;

            List<DNode> gamesLostByPlayerA = new ArrayList<>();
            List<DNode> gamesNotWonByPlayerA = new ArrayList<>();
            notOptimal(gameTree, network, OneOfTwoPlayer.PlayerA, false, gamesLostByPlayerA, gamesNotWonByPlayerA);

            List<DNode> gamesLostByPlayerB = new ArrayList<>();
            List<DNode> gamesNotWonByPlayerB = new ArrayList<>();
            notOptimal(gameTree, network, OneOfTwoPlayer.PlayerB, false, gamesLostByPlayerB, gamesNotWonByPlayerB);
//            if (gamesLostByPlayerB.size() > 0) {
//                renderGame(config, gamesLostByPlayerB.get(0).game);
//            }
//            if (gamesNotWonByPlayerB.size() > 0) {
//                renderGame(config, gamesNotWonByPlayerB.get(0).game);
//            }


            gamesLostByPlayerA = new ArrayList<>();
            gamesNotWonByPlayerA = new ArrayList<>();
            notOptimal(gameTree, network, OneOfTwoPlayer.PlayerA, true, gamesLostByPlayerA, gamesNotWonByPlayerA);

            gamesLostByPlayerB = new ArrayList<>();
            gamesNotWonByPlayerB = new ArrayList<>();
            notOptimal(gameTree, network, OneOfTwoPlayer.PlayerB, true, gamesLostByPlayerB, gamesNotWonByPlayerB);

//            if (gamesNotWonByPlayerB.size() > 0) {
//                renderGame(config, gamesNotWonByPlayerB.get(0).game);
//            }


            //     gamesLostByPlayer(gameTree.rootNode, network, false, OneOfTwoPlayer.PlayerA);
//            gameTree.rootNode.clearAIDecisions();
//            List<DNode> gamesLostByPlayerA = gamesLostByPlayer(gameTree.rootNode, network, true, player);
//            System.out.println("Games lost by " + player.toString() + " with MCTS=" + withMCTS + ": " + gamesLostByPlayer.size());

//            Game game = gamesLostByPlayerA.get(0).getGame();
//                  renderGame(config, game);
//
//            boolean contained = replayBuffer.getBuffer().getData().values().contains(game.getGameDTO());
//            System.out.println("game is contained in buffer: " + contained);


//            gamesLostByPlayer(gameTree.rootNode, network, false, OneOfTwoPlayer.PlayerB);
//            gameTree.rootNode.clearAIDecisions();
//            List<DNode> gamesLostByPlayerB = gamesLostByPlayer(gameTree.rootNode, network, true, OneOfTwoPlayer.PlayerB);

//            if (gamesLostByPlayerB.size() > 0) {
//                Game game = gamesLostByPlayerB.get(0).getGame();
//                renderGame(config, game);
//
//                boolean contained = replayBuffer.getBuffer().getData().containsKey(game.getGameDTO().hashCode());
//                System.out.println("game is contained in buffer: " + contained);
//            }
        }


    }

    private static void notOptimal(GameTree gameTree, Network network, OneOfTwoPlayer player, boolean withMCTS, List<DNode> gamesLostByPlayer, List<DNode> gamesNotWonByPlayer) {
        gameTree.rootNode.clearAIDecisions();
        gameTree.rootNode.addAIDecisions(network, player, withMCTS);


        gameTree.rootNode.collectGamesLost(player, gamesLostByPlayer);
        System.out.println("Games lost by " + player.toString() + " with MCTS=" + withMCTS + ": " + gamesLostByPlayer.size());


//        gameTree.rootNode.collectGamesDrawnThatCouldBeWon(player, gamesNotWonByPlayer);
//        System.out.println("Games drawn (that could be won) by " + player.toString() + " with MCTS=" + withMCTS + ": " + gamesNotWonByPlayer.size());
    }

    private static void nodesWithBadDecisions(Set<DNode> nodesWhereADecisionMatters, Network network, boolean withMCTS) {
        Set<DNode> nodesWithBadDecisions = nodesWhereADecisionMatters.stream()
                .filter(n -> n.isBadDecision(network, withMCTS))
                .collect(Collectors.toSet());
        System.out.println("nodesWithBadDecisions, withMCTS=" + withMCTS + ": " + nodesWithBadDecisions.size());

    }

    private static List<DNode> gamesLostByPlayer(DNode rootNode, Network network, boolean withMCTS, OneOfTwoPlayer player) {
        rootNode.addAIDecisions(network, player, withMCTS);
        List<DNode> gamesLostByPlayerA = new ArrayList<>();
        rootNode.collectGamesLost(player, gamesLostByPlayerA);
        System.out.println("Games lost by " + player.toString() + " with MCTS=" + withMCTS + ": " + gamesLostByPlayerA.size());
        return gamesLostByPlayerA;
    }


}
