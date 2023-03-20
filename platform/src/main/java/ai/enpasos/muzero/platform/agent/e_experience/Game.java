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

package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.c_planning.GumbelSearch;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.episode.Player;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.environment.Environment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.d_model.djl.MyL2Loss.NULL_VALUE;
import static ai.enpasos.muzero.platform.common.ProductPathMax.getProductPathMax;


/**
 * A single episode of interaction with the environment.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public abstract class Game {


    protected boolean purelyRandom;
    @EqualsAndHashCode.Include
    protected GameDTO gameDTO;
    protected MuZeroConfig config;
    protected int actionSpaceSize;
    protected double discount;
    protected Environment environment;
    protected GameDTO originalGameDTO;
    //   @Builder.Default
    List<Double> valueImprovements = new ArrayList<>();
    boolean playedMoreThanOnce;
    double surpriseMean;
    double surpriseMax;

    // boolean done;


    //    public boolean isDone() {
//        return isDone(false);
//    }
    public boolean isDone(boolean replay) {
        return !replay && terminal()
                || !replay && getGameDTO().getActions().size() >= config.getMaxMoves()
                || replay && getOriginalGameDTO().getActions().size() == getGameDTO().getActions().size();
    }


    int epoch;


    GumbelSearch searchManager;
    private Random r;
    private float error;
    private boolean debug;

    private boolean actionApplied;

    //   private PlayTypeKey playTypeKey;

    private boolean reanalyse;

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
            Game game = config.newGame(false, false);
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
        Game copy = getConfig().newGame(this.environment != null, false);
        copy.setGameDTO(this.gameDTO.copy());
        copy.setConfig(this.getConfig());
        copy.setDiscount(this.getDiscount());
        copy.setActionSpaceSize(this.getActionSpaceSize());
        if (environment != null)
            copy.replayToPositionInEnvironment(copy.getGameDTO().getActions().size());
        return copy;
    }


    public @NotNull Game copy(int numberOfActions) {
        Game copy = getConfig().newGame(this.environment != null, false);
        copy.setGameDTO(this.gameDTO.copy(numberOfActions));
        copy.setConfig(this.getConfig());
        copy.setDiscount(this.getDiscount());
        copy.setActionSpaceSize(this.getActionSpaceSize());
        if (environment != null)
            copy.replayToPositionInEnvironment(copy.getGameDTO().getActions().size());
        return copy;
    }


//    public void checkAssumptions() {
//        assertTrue(this.getGameDTO().getPolicyTargets().size() == this.getGameDTO().getActions().size(), "policyTargets.size() == actions.size()");
//    }

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


    // TODO simplify Action handling
    public List<Action> legalActions() {
        List<Action> actionList = new ArrayList<>();
        boolean[] b = this.gameDTO.getLegalActions().get(this.gameDTO.getLegalActions().size() - 1);
        for (int i = 0; i < actionSpaceSize; i++) {
            if (b[i]) {
                actionList.add(config.newAction(i));
            }
        }
        return actionList;
    }

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
        addObservationFromEnvironment();
        addLegalActionFromEnvironment();
        setActionApplied(true);
    }

    public void pseudoApplyFromOriginalGame(Action action) {
        this.getGameDTO().getActions().add(action.getIndex());
        this.getGameDTO().getRewards().add(this.getOriginalGameDTO().getRewards().get(this.getGameDTO().getRewards().size()));
        this.getGameDTO().getObservations().add(this.getOriginalGameDTO().getObservations().get(this.getGameDTO().getObservations().size()));
        this.getGameDTO().getLegalActions().add(this.getOriginalGameDTO().getLegalActions().get(this.getGameDTO().getLegalActions().size()));
        setActionApplied(true);
    }


    public List<Target> makeTarget(int stateIndex, int numUnrollSteps) {
        List<Target> targets = new ArrayList<>();

        IntStream.range(stateIndex, stateIndex + numUnrollSteps + 1).forEach(currentIndex -> {
            Target target = new Target();
            fillTarget(currentIndex, target);
            targets.add(target);
        });
        return targets;
    }


    static List<Double> v0s = new ArrayList<>();


    @SuppressWarnings("java:S3776")
    private void fillTarget(int currentIndex, Target target) {
        if (this.isReanalyse()) {
            int i = 42;
        }
        double value;
        int tdSteps = getTdSteps(currentIndex);
        value = calculateValue(tdSteps, currentIndex);

        float lastReward = getLastReward(currentIndex);

//        if (currentIndex == 0 ) {
//            if (tdSteps != 0) {
//                v0s.add(value);
//                if (v0s.size() > 1000) {
//                    v0s.remove(0);
//                }
//                //System.out.println(">> rawvalue = " + value);
//            } else {
//                if (v0s.size() > 0) {
//                    value = v0s.stream().reduce(0d, Double::sum) / v0s.size();
//                 //   System.out.println(">> value calculated = " + value);
//                }
//            }
//        }


        if (currentIndex < this.getGameDTO().getPolicyTargets().size()) {

            setValueOnTarget(target, value);
            target.setReward(lastReward);
//            if (gameDTO.isHybrid() && tdSteps == 0 && !config.isForTdStep0PolicyTraining()) {
//                target.setPolicy(new float[this.actionSpaceSize]);
//                // the idea is not to put any force on the network to learn a particular action where it is not necessary
//                Arrays.fill(target.getPolicy(), 0f);
//            } else {
            target.setPolicy(this.getGameDTO().getPolicyTargets().get(currentIndex));
            //     }
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
            setValueOnTarget(target, (float) value);
            target.setReward(lastReward);
            target.setPolicy(new float[this.actionSpaceSize]);
            // the idea is not to put any force on the network to learn a particular action where it is not necessary
            Arrays.fill(target.getPolicy(), 0f);
        }

    }

    private int getTdSteps(int currentIndex) {
        int tdSteps;
        if (this.reanalyse) {
            if (gameDTO.isHybrid() && currentIndex < this.getGameDTO().getTHybrid()) {
                tdSteps = 0;
            } else {
                int T = this.getGameDTO().getRewards().size() - 1;
                tdSteps = getTdSteps(currentIndex, T);
            }
        } else {
            if (gameDTO.isHybrid() && currentIndex < this.getGameDTO().getTHybrid()) {
//                int T = this.getGameDTO().getRewards().size() - 1;
//                tdSteps = getTdSteps(currentIndex, T);
                tdSteps = 0;
            } else {
                tdSteps = this.getGameDTO().getTdSteps();
            }
        }
        return tdSteps;
    }

    public int getTdSteps(int currentIndex, int T) {
        if (!config.offPolicyCorrectionOn()) return 0;
        if (this.getGameDTO().getPlayoutPolicy() == null) return 0;
        double b = ThreadLocalRandom.current().nextDouble(0, 1);
        return getTdSteps(b, currentIndex, T);
    }

    public int getTdSteps(double b, int currentIndex, int T) {
        double localPRatioMax = Math.min(this.pRatioMax, config.getOffPolicyRatioLimit());

        int tdSteps;

        if (currentIndex >= T) return 0;

        for (int t = T; t >= currentIndex; t--) {

            double pBase = 1;
            for (int i = currentIndex; i < t; i++) {
                pBase *= this.getGameDTO().getPlayoutPolicy().get(i)[this.getGameDTO().getActions().get(i)];
            }
            double p = 1;
            for (int i = currentIndex; i < t; i++) {
                p *= this.getGameDTO().getPolicyTargets().get(i)[this.getGameDTO().getActions().get(i)];
            }
            double pRatio = p / pBase;
            //  System.out.println(pRatio);
            if (pRatio > b * localPRatioMax) {
                tdSteps = t - currentIndex;
                if (config.allOrNothingOn()) {
                    if (tdSteps != T - currentIndex) { // test for all
                        tdSteps = 0;   // if not all then nothing
                    }
                }
                if (tdSteps > 0)
                    log.trace("tdSteps (>0): " + tdSteps);
                return tdSteps;
            }
        }
        throw new MuZeroNoSampleMatch();
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


    private double calculateValue(int tdSteps, int currentIndex) {
        double value = getBootstrapValue(currentIndex, tdSteps);
        value = addValueFromReward(currentIndex, tdSteps, value);
        return value;
    }


    private double addValueFromReward(int currentIndex, int tdSteps, double value) {
        int bootstrapIndex = currentIndex + tdSteps;
        if (currentIndex > this.getGameDTO().getRewards().size() - 1) {
            int i = this.getGameDTO().getRewards().size() - 1;
            value += (double) this.getGameDTO().getRewards().get(i) * Math.pow(this.discount, i - currentIndex) * getPerspective(i - currentIndex);
        } else {
            for (int i = currentIndex; i < this.getGameDTO().getRewards().size() && i < bootstrapIndex; i++) {
                value += (double) this.getGameDTO().getRewards().get(i) * Math.pow(this.discount, i - currentIndex) * getPerspective(i - currentIndex);
            }
        }
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

    private double getBootstrapValue(int currentIndex, int tdSteps) {
        int bootstrapIndex = currentIndex + tdSteps;
        double value = 0;
        if (gameDTO.isHybrid() || isReanalyse()) {
            if (bootstrapIndex < this.getGameDTO().getRootValuesFromInitialInference().size()) {
                //value = NULL_VALUE;
               value = this.getGameDTO().getRootValuesFromInitialInference().get(bootstrapIndex) * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
            }
        } else {
            if (bootstrapIndex < this.getGameDTO().getRootValueTargets().size()) {
                value = this.getGameDTO().getRootValueTargets().get(bootstrapIndex) * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
            }
        }
        return value;
    }


    public abstract Player toPlay();


    public abstract String render();

    public abstract ObservationModelInput getObservationModelInput(int gamePosision);

    public ObservationModelInput getObservationModelInput() {
        return this.getObservationModelInput(this.gameDTO.getObservations().size() - 1);
    }

    public void addObservationFromEnvironment() {
        gameDTO.getObservations().add(environment.getObservation());
    }

    public void addLegalActionFromEnvironment() {
        List<Action> actions = environment.getLegalActions();
        boolean[] result = new boolean[actionSpaceSize];
        for (int i = 0; i < actions.size(); i++) {
            result[actions.get(i).getIndex()] = true;
        }
        gameDTO.getLegalActions().add(result);
    }

    public abstract void replayToPositionInEnvironment(int stateIndex);

    public @NotNull List<Integer> getRandomActionsIndices(int i) {

        List<Integer> actionList = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            actionList.add(r.nextInt(this.config.getActionSpaceSize()));
        }
        return actionList;
    }


    public abstract void renderNetworkGuess(MuZeroConfig config, Player toPlay, NetworkIO networkIO, boolean b);

    public abstract void renderSuggestionFromPriors(MuZeroConfig config, Node node);

    public abstract void renderMCTSSuggestion(MuZeroConfig config, float[] childVisits);

    public void beforeReplayWithoutChangingActionHistory() {
        this.originalGameDTO = this.gameDTO;
        this.gameDTO = this.gameDTO.copyWithoutActions();
        this.gameDTO.setPolicyTargets(this.originalGameDTO.getPolicyTargets());
        this.gameDTO.getObservations().add(this.originalGameDTO.getObservations().get(0));
        //  this.initEnvironment();
    }

    public void beforeReplayWithoutChangingActionHistory(int backInTime) {
        this.originalGameDTO = this.gameDTO;
        this.gameDTO = this.gameDTO.copy(this.gameDTO.getActions().size() - backInTime);
        this.gameDTO.setPolicyTargets(this.originalGameDTO.getPolicyTargets());
        this.gameDTO.getObservations().add(this.originalGameDTO.getObservations().get(0));
        //   this.initEnvironment();
        //  this.replayToPosition(getGameDTO().getActions().size());
    }


    public void afterReplay() {
        this.setOriginalGameDTO(null);
    }

    public abstract void connectToEnvironment();

    public void initSearchManager(double pRandomActionRawAverage) {
        searchManager = new GumbelSearch(config, this, debug, pRandomActionRawAverage);
    }

    double pRatioMax;

    public double getPRatioMax() {
        int n = getGameDTO().getActions().size();
        int tStart = (int) this.getGameDTO().getTHybrid();
        if (tStart >= n) return 1d;
        double[] pRatios = new double[n - tStart];

        IntStream.range(tStart, n).forEach(i -> {
            int a = getGameDTO().getActions().get(i);
            if (getGameDTO().getPlayoutPolicy().isEmpty()) {
                pRatios[i - tStart] = 1;
            } else {
                pRatios[i - tStart] = getGameDTO().getPolicyTargets().get(i)[a] / getGameDTO().getPlayoutPolicy().get(i)[a];
            }
        });
        double prod = getProductPathMax(pRatios);
        return prod;
    }


}
