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

package ai.enpasos.muzero.gamebuffer;

import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.agent.fast.model.Sample;
import ai.enpasos.muzero.agent.slow.play.*;
import ai.enpasos.muzero.environments.EnvironmentBaseBoardGames;
import ai.enpasos.muzero.environments.OneOfTwoPlayer;
import ai.enpasos.muzero.agent.fast.model.Observation;
import lombok.Data;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

/**
 * A single episode of interaction with the environment.
 */
@Data
public abstract class Game implements Serializable {

    protected boolean purelyRandom;

    protected GameDTO gameDTO;

    protected transient MuZeroConfig config;

    protected int actionSpaceSize;
    protected double discount;
    protected EnvironmentBaseBoardGames environment;


    public Game(@NotNull MuZeroConfig config) {
        this.config = config;
        this.gameDTO = new GameDTO(this);
        this.actionSpaceSize = config.getActionSpaceSize();
        this.discount = config.getDiscount();
    }

    public Game(@NotNull MuZeroConfig config, GameDTO gameDTO) {
        this.config = config;
        this.gameDTO = gameDTO;
        this.actionSpaceSize = config.getActionSpaceSize();
        this.discount = config.getDiscount();
    }

    public static Game decode(@NotNull MuZeroConfig config, byte @NotNull [] bytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            GameDTO dto = (GameDTO) objectInputStream.readObject();
            Game game = config.newGame();
            Objects.requireNonNull(game).setGameDTO(dto);
            return game;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull Game clone() {
        Game clone = decode(this.config, this.encode());
        clone.replayToPosition(clone.getGameDTO().getActionHistory().size());
        return clone;
    }


    public @Nullable Float getLastReward() {
        if (getGameDTO().getRewards().size() == 0) return null;
        return getGameDTO().getRewards().get(getGameDTO().getRewards().size() - 1);
    }

    abstract public boolean terminal();

    abstract public List<Action> legalActions();


    abstract public List<Action> allActionsInActionSpace();

    public void apply(int @NotNull ... actionIndex) {
        Arrays.stream(actionIndex).forEach(
                i -> apply(new Action(config, i))
        );
    }

    public void apply(@NotNull Action action) {
        float reward = this.environment.step(action);

        this.getGameDTO().getRewards().add(reward);
        this.getGameDTO().getActionHistory().add(action.getIndex());
    }


    public void storeSearchStatistics(@NotNull Node root, boolean fastRuleLearning, MinMaxStats minMaxStats) {

        float[] childVisit = new float[this.actionSpaceSize];
        if (fastRuleLearning) {
            for (SortedMap.Entry<Action, Node> e : root.getChildren().entrySet()) {
                Action action = e.getKey();
                Node node = e.getValue();
                childVisit[action.getIndex()] = (float) node.prior;
            }
        } else {

            List<Pair<Action, Double>> distributionInput = MCTS.getDistributionInput(root, config, minMaxStats);
            for (Pair<Action, Double> e : distributionInput) {
                Action action = e.getKey();
                double v = e.getValue();
                childVisit[action.getIndex()] = (float)v;
            }
        }
        this.getGameDTO().getPolicyTarget().add(childVisit);
        this.getGameDTO().getRootValues().add((float) root.valueScore(minMaxStats, config));

    }


    /**
     *   def make_target(self, state_index: int, num_unroll_steps: int, td_steps: int,
     *                   to_play: Player):
     *     # The value target is the discounted root value of the search tree N steps
     *     # into the future, plus the discounted sum of all rewards until then.
     *     targets = []
     *     for current_index in range(state_index, state_index + num_unroll_steps + 1):
     *       bootstrap_index = current_index + td_steps
     *       if bootstrap_index < len(self.root_values):
     *         value = self.root_values[bootstrap_index] * self.discount**td_steps
     *       else:
     *         value = 0
     *
     *       for i, reward in enumerate(self.rewards[current_index:bootstrap_index]):
     *         value += reward * self.discount**i  # pytype: disable=unsupported-operands
     *
     *       if current_index > 0 and current_index <= len(self.rewards):
     *         last_reward = self.rewards[current_index - 1]
     *       else:
     *         last_reward = None
     *
     *       if current_index < len(self.root_values):
     *         targets.append((value, last_reward, self.child_visits[current_index]))
     *       else:
     *         # States past the end of games are treated as absorbing states.
     *         targets.append((0, last_reward, []))
     *     return targets
     */
    // TODO at the moment we are only using the board game case.
    // the implementation also addresses the more general case, but might be not correct in detail
    public @NotNull List<Target> makeTarget(int stateIndex, int numUnrollSteps, int tdSteps, Player toPlay, Sample sample) {

        List<Target> targets = new ArrayList<>();
        int currentIndexPerspective = toPlay == OneOfTwoPlayer.PlayerA ? 1 : -1;

        // TODO naming
        int winnerPerspective = this.getGameDTO().getRewards().size() % 2 == 1 ? 1 : -1;

        for (int currentIndex = stateIndex; currentIndex <= stateIndex + numUnrollSteps; currentIndex++) {
            int bootstrapIndex = currentIndex + tdSteps;
            double value = 0;

            // not happening for boardgames
            if (bootstrapIndex < this.getGameDTO().getRootValues().size()) {
                value = this.getGameDTO().getRootValues().get(bootstrapIndex) * Math.pow(this.discount, tdSteps);
            }

            int startIndex = Math.min(currentIndex, this.getGameDTO().getRewards().size());

            // condition "i < bootstrapIndex" for boardgames true
            for (int i = startIndex; i <= this.getGameDTO().getRewards().size() && i < bootstrapIndex; i++) {
                if (currentIndex == 0) continue;
                value += this.getGameDTO().getRewards().get(i - 1) * Math.pow(this.discount, i - 1) * currentIndexPerspective * winnerPerspective;
            }

            float lastReward = 0f;
            if (currentIndex >= 0 && currentIndex < this.getGameDTO().getRewards().size()) {
                lastReward = this.getGameDTO().getRewards().get(currentIndex);
            }
            Target target = new Target();
            if (currentIndex < this.getGameDTO().getRootValues().size()) {
                target.value = (float) value;
                target.reward = lastReward;
                // TODO check + unit test
//                if (   (currentIndexPerspective == 1 && sample.isActionTrainingPlayerA())
//                    || (currentIndexPerspective == -1 && sample.isActionTrainingPlayerB())
//                )
//                {
                    target.policy = this.getGameDTO().getPolicyTarget().get(currentIndex);
//                } else {
//                    target.policy = new float[this.actionSpaceSize];
//                    // the idea is not to put any policy force on the network if the episode was not "good" == lost
//                    Arrays.fill(target.policy, 0f);
//                }
            }
//            else if (currentIndex == this.getGameDTO().getRootValues().size()) {
//                target.value = 0f; //(float) value;
//                target.reward = lastReward;
//                target.policy = new float[this.actionSpaceSize];
//                // the idea is not to put any force on the network to learn a particular action where it is not necessary
//                Arrays.fill(target.policy, 0f);
//            }
            else {
                target.value = 0f; //(float) value;  // instead of 0  // TODO check
                target.policy = new float[this.actionSpaceSize];
                // the idea is not to put any force on the network to learn a particular action where it is not necessary
                Arrays.fill(target.policy, 0f);
            }
            targets.add(target);
            currentIndexPerspective *= -1;
        }
        return targets;
    }


    abstract public Player toPlay();


    public @NotNull ActionHistory actionHistory() {
        return new ActionHistory(config, this.gameDTO.actionHistory, actionSpaceSize);
    }


    abstract public String render();

    public byte @NotNull [] encode() {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream)) {
            oos.writeObject(this.gameDTO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public abstract Observation getObservation(NDManager ndManager);

    public abstract void replayToPosition(int stateIndex);

    public @NotNull List<Integer> getRandomActionsIndices(int i) {

        Random r = new Random();

        int result = r.nextInt(this.config.getActionSpaceSize());

        List<Action> actions = allActionsInActionSpace();
        List<Integer> actionList = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            actionList.add(r.nextInt(this.config.getActionSpaceSize()));
        }
        return actionList;
    }


    public boolean equals(Object other) {
        if (!(other instanceof Game)) return false;
        Game otherGame = (Game) other;
        return this.getGameDTO().getActionHistory().equals(otherGame.getGameDTO().getActionHistory());
    }

    public int hashCode() {
        return this.getGameDTO().getActionHistory().hashCode();
    }

    public Optional<OneOfTwoPlayer> whoWonTheGame() {
        if (this.getEnvironment().hasPlayerWon(OneOfTwoPlayer.PlayerA)) return  Optional.of(OneOfTwoPlayer.PlayerA);
        if (this.getEnvironment().hasPlayerWon(OneOfTwoPlayer.PlayerB)) return  Optional.of(OneOfTwoPlayer.PlayerB);
       return Optional.empty();
    }

    public boolean hasPositiveOutcomeFor(OneOfTwoPlayer player) {
        // won or draw but not lost
       return !this.getEnvironment().hasPlayerWon(OneOfTwoPlayer.otherPlayer(player));
    }
}
