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

package ai.enpasos.muzero.platform.agent.memorize;

import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.intuitive.Observation;
import ai.enpasos.muzero.platform.agent.intuitive.djl.MyL2Loss;
import ai.enpasos.muzero.platform.agent.rational.*;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.environment.Environment;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.common.Functions.calculateRunningVariance;

/**
 * A single episode of interaction with the environment.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public abstract class Game {

    @Builder.Default
    protected boolean recordValueImprovements = false;
    @Builder.Default
    List<Double> valueImprovements = new ArrayList<>();


    protected boolean purelyRandom;
    @EqualsAndHashCode.Include
    protected GameDTO gameDTO;
    protected MuZeroConfig config;
    protected int actionSpaceSize;
    protected double discount;
    protected Environment environment;
    protected GameDTO originalGameDTO;
    boolean playedMoreThanOnce;
    double surpriseMean;
    double surpriseMax;

    boolean done;


    GumbelSearch searchManager;
    private Random r;
    private float error;
    private boolean debug;

    private boolean actionApplied;

    protected Game(@NotNull MuZeroConfig config) {
        this.config = config;
        this.gameDTO = new GameDTO();
        this.actionSpaceSize = config.getActionSpaceSize();
        this.discount = config.getDiscount();
        r = new Random();
    }

    protected Game(@NotNull MuZeroConfig config, GameDTO gameDTO) {
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
            throw new MuZeroException(e);
        }
    }

    public float calculateSquaredDistanceBetweenOriginalAndCurrentValue() {
        this.error = 0;
        for (int i = 0; i < this.originalGameDTO.getRootValuesFromInitialInference().size(); i++) {
            double d = this.originalGameDTO.getRootValuesFromInitialInference().get(i)
                - this.getGameDTO().getRootValuesFromInitialInference().get(i);
            this.error += d * d;
        }
        return this.error;
    }

    public @NotNull Game copy() {
        Game copy = getConfig().newGame();
        copy.setGameDTO(this.gameDTO.copy());
        copy.setConfig(this.getConfig());
        copy.setDiscount(this.getDiscount());
        copy.setActionSpaceSize(this.getActionSpaceSize());
        copy.replayToPosition(copy.getGameDTO().getActions().size());
        return copy;
    }


    public @NotNull Game copy(int numberOfActions) {
        Game copy = getConfig().newGame();
        copy.setGameDTO(this.gameDTO.copy(numberOfActions));
        copy.setConfig(this.getConfig());
        copy.setDiscount(this.getDiscount());
        copy.setActionSpaceSize(this.getActionSpaceSize());
        copy.replayToPosition(copy.getGameDTO().getActions().size());
        return copy;
    }


    public void checkAssumptions() {
        assertTrue(this.getGameDTO().getPolicyTargets().size() == this.getGameDTO().getActions().size(), "policyTargets.size() == actions.size()");
        assertTrue(this.getGameDTO().getSurprises().size() == this.getGameDTO().getActions().size(), "surprises.size() == actions.size()");
    }

    protected void assertTrue(boolean b, String s) {
        if (b) return;
        log.error(s);
        throw new MuZeroException("assertion violated: " + s);
    }

    public @Nullable Float getLastReward() {
        if (getGameDTO().getRewards().size() == 0) return null;
        return getGameDTO().getRewards().get(getGameDTO().getRewards().size() - 1);
    }

    public abstract boolean terminal();

    public abstract List<Action> legalActions();

    public abstract List<Action> allActionsInActionSpace();

    public void apply(int @NotNull ... actionIndex) {
        Arrays.stream(actionIndex).forEach(
            i -> apply(config.newAction(i))
        );
    }

    public void apply(List<Integer> actions) {
        actions.forEach(
            i -> apply(config.newAction(i))
        );
    }

    public void apply(@NotNull Action action) {
        float reward = this.environment.step(action);

        this.getGameDTO().getRewards().add(reward);
        this.getGameDTO().getActions().add(action.getIndex());
    }

    public List<Target> makeTarget(int stateIndex, int numUnrollSteps) {
        List<Target> targets = new ArrayList<>();

        IntStream.range(stateIndex, stateIndex + numUnrollSteps + 1).forEach(currentIndex -> {
            Target target = new Target();
            fillTarget(currentIndex, stateIndex, target);
            targets.add(target);
        });
        return targets;
    }

    private void fillTarget(int currentIndex, int stateIndex, Target target) {

        if (currentIndex == 0) {
            int k = 42;
        }

        double value = calculateValue(currentIndex, stateIndex);

//        if (currentIndex == 0) {
//            System.out.println("currentIndex = 0, value = " + value);
//        }

        float lastReward = getLastReward(currentIndex);
        int tdSteps = this.getGameDTO().getTdSteps();

        if (currentIndex < this.getGameDTO().getPolicyTargets().size()) {

            setValueOnTarget(target, value);
            target.setReward(lastReward);
            if  (gameDTO.isHybrid() && tdSteps == 0 && !config.isForTdStep0PolicyTraining()) {
                target.setPolicy(new float[this.actionSpaceSize]);
                // the idea is not to put any force on the network to learn a particular action where it is not necessary
                Arrays.fill(target.getPolicy(), 0f);
            } else {
                target.setPolicy(this.getGameDTO().getPolicyTargets().get(currentIndex));
            }
        } else if (!config.isNetworkWithRewardHead() && currentIndex == this.getGameDTO().getPolicyTargets().size()) {
            // If we do not train the reward (as only boardgames are treated here)
            // the value has to take the role of the reward on this node (needed in MCTS)
            // if we were running the network with reward head
            // the value would be 0 here
            // but as we do not get the expected reward from the network
            // we need use this node to keep the reward value
            // therefore target.value is not 0f
            // To make the whole thing clear. The cases with and without a reward head should be treated in a clearer separation

            setValueOnTarget(target, value); // this is not really the value, it is taking the role of the reward here
            target.setReward(lastReward);
            target.setPolicy(new float[this.actionSpaceSize]);
            // the idea is not to put any force on the network to learn a particular action where it is not necessary
            Arrays.fill(target.getPolicy(), 0f);
        } else {
            setValueOnTarget(target, config.isAbsorbingStateDropToZero() ? 0f : (float) value);
            target.setReward(lastReward);
            target.setPolicy(new float[this.actionSpaceSize]);
            // the idea is not to put any force on the network to learn a particular action where it is not necessary
            Arrays.fill(target.getPolicy(), 0f);
        }

    }

    private void setValueOnTarget(Target target, double value) {
        target.setValue((float) value);
    }

    private float getLastReward(int currentIndex) {
        float lastReward;
        if (currentIndex > 0 && currentIndex <= this.getGameDTO().getRewards().size()) {
            lastReward = this.getGameDTO().getRewards().get(currentIndex - 1);
        } else {
            lastReward = 0f;
        }
        return lastReward;
    }


    private double calculateValue(int currentIndex, int stateIndex) {
        int tdSteps = this.getGameDTO().getTdSteps();
        if (gameDTO.isHybrid()) {
            if (stateIndex < this.getGameDTO().getTHybrid()) {
                tdSteps = 0;
            }
        }

        int startIndex;
        int bootstrapIndex = currentIndex + tdSteps;
        double value = getBootstrapValue(tdSteps, bootstrapIndex);
        if (gameDTO.isHybrid() && tdSteps == 0 && !config.isForTdStep0ValueTraining() ) {
            value = MyL2Loss.NULL_VALUE;  // no value change force
        } else {
            if (config.isNetworkWithRewardHead()) {
                startIndex = currentIndex;
            } else {
                startIndex = Math.min(currentIndex, this.getGameDTO().getRewards().size() - 1);
            }
            for (int i = startIndex; i < this.getGameDTO().getRewards().size() && i < bootstrapIndex; i++) {
                value += (double) this.getGameDTO().getRewards().get(i) * Math.pow(this.discount, i) * getPerspective(i - currentIndex);
            }
        }
//        if (gameDTO.isHybrid() && currentIndex == 0 && value == 1.0){
//            System.out.println(" gameDTO.isHybrid() && currentIndex == 0  ... value = " + value);
//        }
//        if (!gameDTO.isHybrid() && currentIndex == 0 ) {
//            System.out.println("!gameDTO.isHybrid() && currentIndex == 0  ... value = " + value);
//        }
        return value;

    }

    private double getPerspective(int delta) {
        boolean perspectiveChange = config.getPlayerMode() == PlayerMode.TWO_PLAYERS;
        double perspective = 1.0;
        if (perspectiveChange) {
            perspective = Math.pow(-1, delta);
        }
        return perspective;
    }

    private double getBootstrapValue(int tdSteps, int bootstrapIndex) {
        double value;
        if (bootstrapIndex < this.getGameDTO().getRootValueTargets().size()) {
            value = this.getGameDTO().getRootValueTargets().get(bootstrapIndex) * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
        } else {
            value = 0;
        }
        return value;
    }


    public abstract Player toPlay();

    public @NotNull ActionHistory actionHistory() {
        return new ActionHistory(config, this.gameDTO.getActions(), actionSpaceSize);
    }

    public abstract String render();

    public abstract Observation getObservation(NDManager ndManager);

    public abstract void replayToPosition(int stateIndex);

    public @NotNull List<Integer> getRandomActionsIndices(int i) {

        List<Integer> actionList = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            actionList.add(r.nextInt(this.config.getActionSpaceSize()));
        }
        return actionList;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Game)) return false;
        Game otherGame = (Game) other;
        return this.getGameDTO().getActions().equals(otherGame.getGameDTO().getActions());
    }

    public int hashCode() {
        return this.getGameDTO().getActions().hashCode();
    }

    public abstract void renderNetworkGuess(MuZeroConfig config, Player toPlay, NetworkIO networkIO, boolean b);

    public abstract void renderSuggestionFromPriors(MuZeroConfig config, Node node);

    public abstract void renderMCTSSuggestion(MuZeroConfig config, float[] childVisits);

    public void beforeReplayWithoutChangingActionHistory() {
        this.originalGameDTO = this.gameDTO;
        this.gameDTO = this.gameDTO.copyWithoutActions();
        this.gameDTO.setPolicyTargets(this.originalGameDTO.getPolicyTargets());
        this.initEnvironment();
    }

    public void beforeReplayWithoutChangingActionHistory(int backInTime) {
        this.originalGameDTO = this.gameDTO;
        this.gameDTO = this.gameDTO.copy(this.gameDTO.getActions().size() - backInTime);
        this.gameDTO.setPolicyTargets(this.originalGameDTO.getPolicyTargets());
        this.initEnvironment();
        this.replayToPosition(getGameDTO().getActions().size());
    }


    public void afterReplay() {
        this.setOriginalGameDTO(null);
    }

    public abstract void initEnvironment();

    public void initSearchManager(double pRandomActionRawAverage) {
        searchManager = new GumbelSearch(config, this, debug, pRandomActionRawAverage);
    }

    public double calculateValueImprovementVariance(MuZeroConfig config) {
        return calculateRunningVariance(this.getValueImprovements(), config);
    }


}
