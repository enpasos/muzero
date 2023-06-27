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

import ai.enpasos.muzero.platform.agent.e_experience.ZeroSumGame;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Data
public class DNode {
    ZeroSumGame game;
    boolean terminal;
    DNode parent;
    List<DNode> children;

    @Nullable DNode aiChosenChild;

    boolean decisionMatters;


    Integer bestForceableValuePlayerA;
    Integer bestForceableValuePlayerB;


    public DNode(ZeroSumGame game) {
        this.game = game;
        children = new ArrayList<>();
        terminal = false;
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
        if (player == OneOfTwoPlayer.PLAYER_A) {
            bestForceableValuePlayerA = value;
        } else {
            bestForceableValuePlayerB = value;
        }
    }


    public void findNodesWhereADecisionMatters(@NotNull OneOfTwoPlayer player, @NotNull Set<DNode> nodesWhereADecisionMatters) {

        for (DNode child : children) {
            child.findNodesWhereADecisionMatters(player, nodesWhereADecisionMatters);
        }
        if (this.game.toPlay() == player && this.getChildren().size() > 1 && this.getChildren().stream().mapToInt(n -> n.getBestForceableValue(player)).distinct().count() > 1) {
            nodesWhereADecisionMatters.add(this);
            decisionMatters = true;
        } else {
            decisionMatters = false;
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


    public void expand(@NotNull List<DNode> nonExpandedGameNodes, @NotNull List<DNode> terminalGameNodes) {
        if (!game.terminal() && !game.legalActions().isEmpty()) {
            for (Action action : game.legalActions()) {
                ZeroSumGame newGame = (ZeroSumGame) game.copy();
                newGame.apply(action);
                DNode child = new DNode(this, newGame);
                this.children.add(child);
                nonExpandedGameNodes.add(child);
            }
        } else {
            terminalGameNodes.add(this);
            terminal = true;
        }
        nonExpandedGameNodes.remove(this);
    }


    public void collectNonTerminalNodes(@NotNull Set<DNode> nonTerminalNodes) {
        if (!this.children.isEmpty()) {
            nonTerminalNodes.add(this);
            this.children.forEach(n -> n.collectNonTerminalNodes(nonTerminalNodes));
        }
    }


    public int hashCode() {
        return Arrays.deepHashCode(game.getEnvironment().getBoard());
    }

    /**
     * Two nodes are equal if they have the same state.
     * Here we are switching implicitly from tree to acyclic graph.
     * Comparison has an acyclic directed graph in mind.
     */
    public boolean equals(Object o) {
        if (!(o instanceof DNode n)) return false;

        return Arrays.deepEquals(
            n.game.getEnvironment().getBoard(),
            game.getEnvironment().getBoard());

    }


    public void propagateBestForceableValueUp(@NotNull OneOfTwoPlayer player) {
        for (DNode n : this.children) {
            if (n.getBestForceableValue(player) == null) {
                n.propagateBestForceableValueUp(player);
            }
        }
        if (this.getBestForceableValue(player) == null) {
            if (this.game.toPlay() == player) {
                // the player decides -> max
                this.setBestForceableValue(player, this.children.stream().
                    max(Comparator.comparing(n -> n.getBestForceableValue(player)))
                    .orElseThrow(MuZeroException::new).getBestForceableValue(player));
            } else {
                // the opponent decides -> min
                this.setBestForceableValue(player, this.children.stream().
                    min(Comparator.comparing(n -> n.getBestForceableValue(player)))
                    .orElseThrow(MuZeroException::new).getBestForceableValue(player));
            }
        }
    }


    public DNode getChild(int action) {
        for (DNode n : children) {
            int t = n.game.getEpisodeDO().getLastActionTime();
            if (n.game.getEpisodeDO().getTimeSteps().get(t).getAction() == action) {
                return n;
            }
        }
        return null;
    }

    public  boolean isOnOptimalPath(DNode root) {
        return this.bestForceableValuePlayerA.equals(root.bestForceableValuePlayerA)
            && this.bestForceableValuePlayerB.equals(root.bestForceableValuePlayerB);
    }
}
