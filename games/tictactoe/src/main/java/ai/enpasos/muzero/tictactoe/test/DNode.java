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

import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.platform.agent.gamebuffer.ZeroSumGame;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.agent.slow.play.MCTS;
import ai.enpasos.muzero.platform.agent.slow.play.MinMaxStats;
import ai.enpasos.muzero.platform.agent.slow.play.Node;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Data
public class DNode {
    ZeroSumGame game;
    boolean terminated;
    DNode parent;
    List<DNode> children;
    boolean checked;
    @Nullable DNode aiChosenChild;
    double aiValue;
    boolean decisionMatters;

    // let us define the perfect value of a node
    // 1 if the player to play can force a win
    // 0 if the player to play can not force a win but can force a draw
    // -1 else
    @Nullable Integer perfectValue;
    Integer bestForceableValuePlayerA;
    Integer bestForceableValuePlayerB;

    // the value the worst decision would lead to
    @Nullable Integer worstValue;

    public DNode(ZeroSumGame game) {
        this.game = game;
        children = new ArrayList<>();
        terminated = false;
    }

    public DNode(DNode parent, ZeroSumGame game) {
        this(game);
        this.parent = parent;
    }

    public @Nullable Integer getBestForceableValue(@NotNull OneOfTwoPlayer player) {
        switch (player) {
            case PLAYER_A:
                return bestForceableValuePlayerA;
            case PLAYER_B:
                return bestForceableValuePlayerB;
            default:
                return null;
        }
    }

    public void setBestForceableValue(@NotNull OneOfTwoPlayer player, Integer value) {
        switch (player) {
            case PLAYER_A:
                bestForceableValuePlayerA = value;
                break;
            case PLAYER_B:
                bestForceableValuePlayerB = value;
                break;
            default:
        }
    }

    public void clearPerfectValuesOnNodeAndDescendants() {
        this.perfectValue = null;
        this.worstValue = null;
        children.forEach(DNode::clearPerfectValuesOnNodeAndDescendants);
    }

    public void findNodesWhereADecisionMatters(@NotNull Set<DNode> nodesWhereADecisionMatters) {
        clearPerfectValuesOnNodeAndDescendants();
        setPerfectValuesOnDescendants(nodesWhereADecisionMatters);
    }

    private void setPerfectValuesOnDescendants(@NotNull Set<DNode> nodesWhereADecisionMatters) {
        if (this.children.isEmpty()) {
            Integer value;
            value = getValue();
            this.perfectValue = value;
            this.worstValue = perfectValue;
        } else {
            this.children.forEach(n -> n.setPerfectValuesOnDescendants(nodesWhereADecisionMatters));
            // now all the descendants have it
            // we are looking for options to force that means assuming that the opponent could take the best option
            this.perfectValue = -1 * this.children.stream().min(Comparator.comparing(DNode::getPerfectValue)).orElseThrow(MuZeroException::new).perfectValue;
            this.worstValue = -1 * this.children.stream().max(Comparator.comparing(DNode::getPerfectValue)).orElseThrow(MuZeroException::new).perfectValue;
            if (!Objects.equals(this.perfectValue, this.worstValue)) {
                nodesWhereADecisionMatters.add(this);
                decisionMatters = true;
            } else {
                decisionMatters = false;
            }
        }
    }

    public Integer getValue(OneOfTwoPlayer player) {
        int value;
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

    public void expand(@NotNull List<DNode> unterminatedGameNodes, @NotNull List<DNode> terminatedGameNodes) {
        if (!game.terminal() && !game.legalActions().isEmpty()) {
            for (Action action : game.legalActions()) {
                ZeroSumGame newGame = (ZeroSumGame) game.copy();
                newGame.apply(action);
                DNode child = new DNode(this, newGame);
                this.children.add(child);
                unterminatedGameNodes.add(child);
            }
        } else {
            terminatedGameNodes.add(this);
            terminated = false;
        }
        unterminatedGameNodes.remove(this);
    }


    // add AI Decisions on all descendant nodes
    // where 'player' is to play
    public void addAIDecisions(@NotNull Network network, OneOfTwoPlayer player, boolean withMCTS, MCTS mcts) {
        if (this.game.toPlay() != player) {   // jump over other players nodes down the tree
            for (DNode node : this.children) {
                node.addAIDecisions(network, player, withMCTS, mcts);
            }
        } else if (!this.children.isEmpty()) {
            this.aiChosenChild = aiDecision(network, withMCTS, mcts);
            Objects.requireNonNull(aiChosenChild).addAIDecisions(network, player, withMCTS, mcts);
        }
    }

    public @Nullable DNode aiDecision(@NotNull Network network, boolean withMCTS, MCTS mcts) {
        NetworkIO networkOutput = network.initialInferenceDirect(game);
        aiValue = networkOutput.getValue();
        int actionIndexSelectedByNetwork = -1;
        List<Action> legalActions = game.legalActions();
        if (withMCTS) {
            Node root = new Node(network.getConfig(), 0);


            mcts.expandNode(root, game.toPlay(), legalActions, networkOutput, false);

            MinMaxStats minMaxStats = mcts.run(root, game.actionHistory(), network, null);

            Action action = mcts.selectActionByMax(root, minMaxStats);
            actionIndexSelectedByNetwork = action.getIndex();
        } else {
            float maxValue = 0f;
            for (int i = 0; i < networkOutput.getPolicyValues().length; i++) {
                float v = networkOutput.getPolicyValues()[i];
                if (v > maxValue && legalActions.contains(network.getConfig().newAction(i))) {
                    maxValue = v;
                    actionIndexSelectedByNetwork = i;
                }
            }
        }
        for (DNode n : children) {
            if (n.game.actionHistory().lastAction().getIndex() == actionIndexSelectedByNetwork) {
                return n;
            }
        }
        return null;
    }

    public void collectGamesLost(OneOfTwoPlayer player, @NotNull List<DNode> gamesLostByPlayer) {
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
        children.forEach(DNode::clearAIDecisions);
    }

    public void collectDecisionNodes(@NotNull Set<DNode> decisionNodes) {
        if (!this.children.isEmpty()) {
            decisionNodes.add(this);
            this.children.forEach(n -> n.collectDecisionNodes(decisionNodes));
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

    public boolean isBadDecision(@NotNull Network network, boolean withMCTS, MCTS mcts) {
        this.aiChosenChild = aiDecision(network, withMCTS, mcts);
        int chosenValue = -1 * Objects.requireNonNull(this.aiChosenChild).perfectValue;
        return chosenValue != perfectValue;
    }

    public void propagateBestForceableValueUp(@NotNull OneOfTwoPlayer player) {
        for (DNode n : this.children) {
            if (n.getBestForceableValue(player) == null) {
                n.propagateBestForceableValueUp(player);
            }
        }
        if (this.getBestForceableValue(player) == null) {
            if (this.game.toPlay() == player) {
                // the player decides
                this.setBestForceableValue(player, this.children.stream().
                        max(Comparator.comparing(n -> n.getBestForceableValue(player)))
                        .orElseThrow(MuZeroException::new).getBestForceableValue(player));
            } else {
                // the opponent decides
                this.setBestForceableValue(player, this.children.stream().
                        min(Comparator.comparing(n -> n.getBestForceableValue(player)))
                        .orElseThrow(MuZeroException::new).getBestForceableValue(player));
            }
        }
    }

    public void collectForceableWinNodes(@NotNull OneOfTwoPlayer player, @NotNull List<DNode> forceableWinNodesPlayer) {
        if (this.getBestForceableValue(player) == 1 && this.getGame().toPlay() == player && this.getChildren() != null && this.decisionMatters) {
            forceableWinNodesPlayer.add(this);
        }
        this.children.forEach(n -> n.collectForceableWinNodes(player, forceableWinNodesPlayer));
    }


    public boolean hasAncestorWithForceableWin(@NotNull OneOfTwoPlayer player) {
        if (this.parent != null && this.parent.getBestForceableValue(player) == 1) return true;
        if (this.parent != null) {
            return this.parent.hasAncestorWithForceableWin(player);
        }
        return false;
    }

    public void collectGamesDrawnThatCouldBeWon(@NotNull OneOfTwoPlayer player, @NotNull List<DNode> gamesNotWonByPlayer) {

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
