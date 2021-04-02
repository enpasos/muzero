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

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.environments.OneOfTwoPlayer;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.network.Network;
import ai.enpasos.muzero.network.NetworkIO;
import ai.enpasos.muzero.play.Action;
import ai.enpasos.muzero.play.MCTS;
import ai.enpasos.muzero.play.Node;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static ai.enpasos.muzero.play.PlayManager.getAllActionsOnDevice;

@Data
public class DNode {
    Game game;
    //   List<Game> gamesFromNode;
    boolean terminated;
    DNode parent;
    List<DNode> children;
    boolean checked;
    DNode aiChosenChild;
    double aiValue;
    boolean decisionMatters;

    // let us define the perfect value of a node
    // 1 if the player to play can force a win
    // 0 if the player to play can not force a win but can force a draw
    // -1 else
    Integer perfectValue;
    Integer bestForceableValuePlayerA;
    Integer bestForceableValuePlayerB;

    // the value the worst decision would lead to
    Integer worstValue;

    public DNode(Game game) {
        this.game = game;
        //    gamesFromNode = new ArrayList<>();
        children = new ArrayList<>();
        terminated = false;
    }

    public DNode(DNode parent, Game game) {
        this(game);
        this.parent = parent;
    }

    public Integer getBestForceableValue(OneOfTwoPlayer player) {
        switch (player) {
            case PlayerA:
                return bestForceableValuePlayerA;
            case PlayerB:
                return bestForceableValuePlayerB;
        }
        return null;
    }

    public void setBestForceableValue(OneOfTwoPlayer player, Integer value) {
        switch (player) {
            case PlayerA:
                bestForceableValuePlayerA = value;
                break;
            case PlayerB:
                bestForceableValuePlayerB = value;
                break;
        }
    }

    public void clearPerfectValuesOnNodeAndDescendants() {
        this.perfectValue = null;
        this.worstValue = null;
        children.stream().forEach(n -> n.clearPerfectValuesOnNodeAndDescendants());
    }

    public void findNodesWhereADecisionMatters(Set<DNode> nodesWhereADecisionMatters) {
        clearPerfectValuesOnNodeAndDescendants();
        setPerfectValuesOnDescendants(nodesWhereADecisionMatters);
    }

    private void setPerfectValuesOnDescendants(Set<DNode> nodesWhereADecisionMatters) {
        if (this.children.size() == 0) {
            Integer value = null;
            value = getValue();
            this.perfectValue = value;
            this.worstValue = perfectValue;
        } else {
            this.children.stream().forEach(n -> n.setPerfectValuesOnDescendants(nodesWhereADecisionMatters));
            // now all the descendants have it
            // we are looking for options to force that means assuming that the opponent could take the best option
            this.perfectValue = -1 * this.children.stream().min(Comparator.comparing(DNode::getPerfectValue)).get().perfectValue;
            this.worstValue = -1 * this.children.stream().max(Comparator.comparing(DNode::getPerfectValue)).get().perfectValue;
            if (this.perfectValue != this.worstValue) {
                nodesWhereADecisionMatters.add(this);
                decisionMatters = true;
            } else {
                decisionMatters = false;
            }
        }
    }

    public Integer getValue(OneOfTwoPlayer player) {
        Integer value;
        OneOfTwoPlayer opponent = OneOfTwoPlayer.otherPlayer(player);
        if (this.game.getEnvironment().hasPlayerWon(player)) {
            value = 1;
        } else if (this.game.getEnvironment().hasPlayerWon(opponent)) {
            value = -1;
        } else {
            value = 0;
        }
        return value;
    }

    public Integer getValue() {
        OneOfTwoPlayer player = (OneOfTwoPlayer) this.game.toPlay();
        return getValue(player);
    }

    public void expand(List<DNode> unterminatedGameNodes, List<DNode> terminatedGameNodes) {
        if (!game.terminal() && game.legalActions().size() > 0) {
            for (Action action : game.legalActions()) {
                Game newGame = game.clone();
                newGame.apply(action);
                DNode child = new DNode(this, newGame);
                this.children.add(child);
                unterminatedGameNodes.add(child);
                // gamesFromNode.add(newGame);
            }
        } else {
            terminatedGameNodes.add(this);
            terminated = false;
        }
        unterminatedGameNodes.remove(this);
    }


    // add AI Decisions on all descendant nodes
    // where 'player' is to play
    public void addAIDecisions(Network network, OneOfTwoPlayer player, boolean withMCTS) {
        if (this.game.toPlay() != player) {   // jump over other players nodes down the tree
            for (DNode node : this.children) {
                node.addAIDecisions(network, player, withMCTS);
            }
        } else if (this.children.size() > 0) {
            //    if (hasDescendantWherePlayerWon(OneOfTwoPlayer.otherPlayer(player))) {
            this.aiChosenChild = aiDecision(network, withMCTS);
//            if (this.aiChosenChild == null) {
//                int i = 42;
//            }
            // jump over opponents node
            // for (DNode node : this.aiChosenChild.getChildren()) {
            aiChosenChild.addAIDecisions(network, player, withMCTS);
            //   }
        }
    }

//    private boolean hasDescendantWherePlayerWon(OneOfTwoPlayer player) {
//        if (this.children.size() == 0) {
//            return false;
//        } else {
//            for (DNode child : this.children) {
//                if (child.game.getEnvironment().hasPlayerWon(player)) return true;
//                if (child.hasDescendantWherePlayerWon(player)) {
//                    return true;
//                }
//            }
//            return false;
//        }
//    }

    public DNode aiDecision(Network network, boolean withMCTS) {
        NetworkIO networkOutput = network.initialInference(game.getObservation(network.getNDManager()));
        aiValue = networkOutput.getValue();
        int actionIndexSelectedByNetwork = -1;
        if (!withMCTS) {
            float maxValue = 0f;
            for (int i = 0; i < networkOutput.getPolicyValues().length; i++) {
                float v = networkOutput.getPolicyValues()[i];
                if (v > maxValue) {
                    maxValue = v;
                    actionIndexSelectedByNetwork = i;
                }
            }
        } else {
            Node root = new Node(0);
            MCTS mcts = new MCTS(this.game.getConfig());
            List<Action> legalActions = game.legalActions();
            mcts.expandNode(root, game.toPlay(), legalActions, networkOutput, false);
            List<NDArray> actionSpaceOnDevice = getAllActionsOnDevice(network.getConfig(), network.getNDManager());
            mcts.run(root, game.actionHistory(), network, null, actionSpaceOnDevice);

            Action action = mcts.selectActionByMaxFromDistribution(game.getGameDTO().getActionHistory().size(), root, network);
            actionIndexSelectedByNetwork = action.getIndex();
        }
        for (DNode n : children) {
            if (n.game.actionHistory().lastAction().getIndex() == actionIndexSelectedByNetwork) {
                return n;
            }
        }
        return null;
    }

    public void collectGamesLost(OneOfTwoPlayer player, List<DNode> gamesLostByPlayer) {
        OneOfTwoPlayer opponent = OneOfTwoPlayer.otherPlayer(player);
        if (this.game.terminal()) {
            if (this.game.getEnvironment().hasPlayerWon(opponent)) {
                gamesLostByPlayer.add(this);
            }
        } else {
            if (this.aiChosenChild == null) {
                for (DNode node : this.getChildren()) {
                    node.collectGamesLost(player, gamesLostByPlayer);
                }
            } else {
                this.aiChosenChild.collectGamesLost(player, gamesLostByPlayer);
            }
        }
    }

    public void clearAIDecisions() {
        aiChosenChild = null;
        children.stream().forEach(n -> n.clearAIDecisions());
    }

    public void collectDecisionNodes(Set<DNode> decisionNodes) {
        if (this.children.size() > 0) {
            decisionNodes.add(this);
            this.children.stream().forEach(n -> n.collectDecisionNodes(decisionNodes));
        }
    }

    public int hashCode() {
        return this.game.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof DNode)) return false;
        DNode n = (DNode) o;
        return n.game.equals(n.getGame());
    }

    public boolean isBadDecision(Network network, boolean withMCTS) {
        this.aiChosenChild = aiDecision(network, withMCTS);
        int chosenValue = -1 * this.aiChosenChild.perfectValue;
        return chosenValue != perfectValue;
    }

    public void propagateBestForceableValueUp(OneOfTwoPlayer player) {
        for (DNode n : this.children) {
            if (n.getBestForceableValue(player) == null) {
                n.propagateBestForceableValueUp(player);
            }
        }
        if (this.getBestForceableValue(player) == null) {
            //if (this.getBestForceableValue(player) != null) return; // already set on node
            if (this.game.toPlay() == player) {
                // the player decides
                this.setBestForceableValue(player, this.children.stream().
                        max((n1, n2) -> n1.getBestForceableValue(player).compareTo(n2.getBestForceableValue(player)))
                        .get().getBestForceableValue(player));
            } else {
                // the opponent decides
                this.setBestForceableValue(player, this.children.stream().
                        min((n1, n2) -> n1.getBestForceableValue(player).compareTo(n2.getBestForceableValue(player)))
                        .get().getBestForceableValue(player));
            }
        }
    }

    public void collectForceableWinNodes(OneOfTwoPlayer player, List<DNode> forceableWinNodesPlayer) {
        if (this.getBestForceableValue(player) == 1 && this.getGame().toPlay() == player && this.getChildren() != null && this.decisionMatters) {
            forceableWinNodesPlayer.add(this);
        }
        this.children.forEach(n -> n.collectForceableWinNodes(player, forceableWinNodesPlayer));
    }


    public boolean hasAncestorWithForceableWin(OneOfTwoPlayer player) {
        if (this.parent != null && this.parent.getBestForceableValue(player) == 1) return true;
        if (this.parent != null) {
            return this.parent.hasAncestorWithForceableWin(player);
        }
        return false;
    }

    public void collectGamesDrawnThatCouldBeWon(OneOfTwoPlayer player, List<DNode> gamesNotWonByPlayer) {

        OneOfTwoPlayer opponent = OneOfTwoPlayer.otherPlayer(player);
        if (this.game.terminal()) {
            if (!this.game.getEnvironment().hasPlayerWon(player) && !this.game.getEnvironment().hasPlayerWon(opponent) && this.parent.game.toPlay() == player && hasAncestorWithForceableWin(player)) {
                gamesNotWonByPlayer.add(this);
            }
        } else {
            if (this.aiChosenChild == null) {
                for (DNode node : this.getChildren()) {
                    node.collectGamesDrawnThatCouldBeWon(player, gamesNotWonByPlayer);
                }
            } else {
                this.aiChosenChild.collectGamesDrawnThatCouldBeWon(player, gamesNotWonByPlayer);
            }
        }
    }
}
