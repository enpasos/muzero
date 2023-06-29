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
import ai.enpasos.muzero.platform.agent.b_episode.Player;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.environment.Environment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.common.ProductPathMax.getProductPathMax;


/**
 * A single episode of interaction with the environment.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public abstract class Game {


    static List<Double> v0s = new ArrayList<>();
    protected boolean purelyRandom;
    @EqualsAndHashCode.Include
    protected EpisodeDO episodeDO;
    protected MuZeroConfig config;
    protected int actionSpaceSize;
    protected double discount;
    protected Environment environment;
    protected EpisodeDO originalEpisodeDO;
    protected TimeStepDO currentTimeStepDO;
    //   @Builder.Default
    List<Double> valueImprovements = new ArrayList<>();
    boolean playedMoreThanOnce;
    double surpriseMean;
    double surpriseMax;
    int epoch;


    GumbelSearch searchManager;
    double pRatioMax;
    private Random r;
    private float error;
    private boolean debug;
    private boolean actionApplied;


    private boolean reanalyse;
    private int tReanalyseMin;


    protected Game(@NotNull MuZeroConfig config) {
        this.config = config;
      //  this.gameDTO = new GameDTO();
        this.episodeDO = new EpisodeDO();
        this.actionSpaceSize = config.getActionSpaceSize();
        this.discount = config.getDiscount();

        r = new Random();
    }

    protected Game(@NotNull MuZeroConfig config, EpisodeDO episodeDO) {
        this.config = config;
        this.episodeDO = episodeDO;
        this.actionSpaceSize = config.getActionSpaceSize();
        this.discount = config.getDiscount();
    }



    public boolean isDone(boolean replay) {
        return !replay && terminal()
                || !replay && getEpisodeDO().getLastTimeWithAction() + 1 >= config.getMaxMoves()
                || replay && getOriginalEpisodeDO().getLastTimeWithAction() == getEpisodeDO().getLastTimeWithAction();
    }


    public @NotNull Game copy() {
        Game copy = getConfig().newGame(this.environment != null, false);
        copy.setEpisodeDO(this.episodeDO.copy());
        copy.setConfig(this.getConfig());
        copy.setDiscount(this.getDiscount());
        copy.setActionSpaceSize(this.getActionSpaceSize());
        if (environment != null)
            copy.replayToPositionInEnvironment(copy.getEpisodeDO().getLastTimeWithAction()+1);  // todo: check
        return copy;
    }

    public @NotNull Game copy(int numberOfActions) {
        Game copy = getConfig().newGame(this.environment != null, false);
        copy.setEpisodeDO(this.episodeDO.copy(numberOfActions));
        copy.setConfig(this.getConfig());
        copy.setDiscount(this.getDiscount());
        copy.setActionSpaceSize(this.getActionSpaceSize());
        if (environment != null)
            copy.replayToPositionInEnvironment(copy.getEpisodeDO().getLastTimeWithAction()+1);
        return copy;
    }

    protected void assertTrue(boolean b, String s) {
        if (b) return;
        log.error(s);
        throw new MuZeroException("assertion violated: " + s);
    }



    public abstract boolean terminal();

    // TODO simplify Action handling
    public List<Action> legalActions() {
        boolean[] b = this.episodeDO.getLegalActionsFromLastTimeStep();

        List<Action> actionList = new ArrayList<>();
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
        this.getEpisodeDO().getLastTimeStep().setReward(reward);
        this.getEpisodeDO().getLastTimeStep().setAction(action.getIndex());

        // now ... observation and legal actions already belong to the next timestamp
        getEpisodeDO().addNewTimeStepDO();
        addObservationFromEnvironment();
        addLegalActionFromEnvironment();
        setActionApplied(true);
    }

    public void pseudoApplyFromOriginalGame(Action action) {
        this.getEpisodeDO().getLastTimeStep().setAction(action.getIndex());
        this.getEpisodeDO().getLastTimeStep().setReward(this.getOriginalEpisodeDO().getRewardFromLastTimeStep());
        // new time
        getEpisodeDO().addNewTimeStepDO();
        this.getEpisodeDO().getLastTimeStep().setObservation(this.getOriginalEpisodeDO().getObservationFromLastTimeStep());
        this.getEpisodeDO().getLastTimeStep().setLegalActions(this.getOriginalEpisodeDO().getLegalActionsFromLastTimeStep());
        setActionApplied(true);
    }

    public List<Target> makeTarget(int stateIndex, int numUnrollSteps, boolean isEntropyContributingToReward) {
        List<Target> targets = new ArrayList<>();

        IntStream.range(stateIndex, stateIndex + numUnrollSteps + 1).forEach(currentIndex -> {
            Target target = new Target();
            fillTarget(currentIndex, target, isEntropyContributingToReward);
            targets.add(target);
        });
        return targets;
    }

    @SuppressWarnings("java:S3776")
    private void fillTarget(int currentIndex, Target target, boolean isEntropyContributingToReward) {

        int tdSteps = getTdSteps(currentIndex);
        double value = calculateValue(tdSteps, currentIndex);
       // double entropyValue = calculateEntropyValue(tdSteps, currentIndex);
        float reward = getReward(currentIndex);


        if (currentIndex < this.getEpisodeDO().getLastTimeWithAction() + 1) {
        //    target.setEntropyValue((float) entropyValue);
            target.setValue((float) value);
            target.setReward(reward);
            target.setPolicy(this.getEpisodeDO().getTimeSteps().get(currentIndex).getPolicyTarget());
        } else if (!config.isNetworkWithRewardHead() && currentIndex == this.getEpisodeDO().getLastTimeWithAction() + 1) {
            // If we do not train the reward (as only boardgames are treated here)
            // the value has to take the role of the reward on this node (needed in MCTS)
            // if we were running the network with reward head
            // the value would be 0 here
            // but as we do not get the expected reward from the network
            // we need use this node to keep the reward value
            // therefore target.value is not 0f
            // To make the whole thing clear. The cases with and without a reward head should be treated in a clearer separation

           // target.setEntropyValue((float) entropyValue);
            target.setValue((float) value); // this is not really the value, it is taking the role of the reward here
            target.setReward(reward);
            target.setPolicy(new float[this.actionSpaceSize]);
            // the idea is not to put any force on the network to learn a particular action where it is not necessary
            Arrays.fill(target.getPolicy(), 0f);
        } else {
          //  target.setEntropyValue((float) entropyValue);
            target.setValue((float) value);
            target.setReward(reward);
            target.setPolicy(new float[this.actionSpaceSize]);
            // the idea is not to put any force on the network to learn a particular action where it is not necessary
            Arrays.fill(target.getPolicy(), 0f);
        }

    }

    private int getTdSteps(int currentIndex) {
        int tdSteps;
        if (this.reanalyse) {
            if (episodeDO.isHybrid() && isItExplorationTime(currentIndex)) {
                tdSteps = 0;
            } else {
                int tMaxHorizon = episodeDO.getLastTime();
                tdSteps = getTdSteps(currentIndex, tMaxHorizon);
            }
        } else {
            if (episodeDO.isHybrid() && isItExplorationTime(currentIndex)) {
                tdSteps = 0;
            } else {
                tdSteps = episodeDO.getTdSteps();
            }
        }
        return tdSteps;
    }

    public int getTdSteps(int currentIndex, int tMaxHorizon) {
        if (!config.offPolicyCorrectionOn()) return 0;
        if (episodeDO.getFirstTimeStep().orElseThrow().getPlayoutPolicy() == null) return 0;
        double b = ThreadLocalRandom.current().nextDouble(0, 1);
        return getTdSteps(b, currentIndex, tMaxHorizon);
    }

    public int getTdSteps(double b, int currentIndex, int tMaxHorizon) {
        double localPRatioMax = Math.min(this.pRatioMax, config.getOffPolicyRatioLimit());

        int tdSteps;

        if (currentIndex < tReanalyseMin) {
            throw new MuZeroNoSampleMatch();
        }

        if (currentIndex >= tMaxHorizon  ) return 0;

        for (int t = tMaxHorizon; t >= currentIndex; t--) {

            double pBase = 1;
            for (int i = currentIndex; i < t; i++) {
                TimeStepDO timeStepDO = this.getEpisodeDO().getTimeSteps().get(i);
                pBase *= timeStepDO.getPlayoutPolicy()[timeStepDO.getAction()];
            }
            double p = 1;
            for (int i = currentIndex; i < t; i++) {
                TimeStepDO timeStepDO = this.getEpisodeDO().getTimeSteps().get(i);
                p *= timeStepDO.getPolicyTarget()[timeStepDO.getAction()];
            }
            double pRatio = p / pBase;
            if (pRatio > b * localPRatioMax) {
                tdSteps = t - currentIndex;
                if (config.allOrNothingOn() && tdSteps != tMaxHorizon - currentIndex) {
                    tdSteps = 0;   // if not all then nothing
                }
                if (tdSteps > 0)
                    log.trace("tdSteps (>0): " + tdSteps);
                return tdSteps;
            }
        }
        throw new MuZeroNoSampleMatch();
    }

    private float getReward(int currentIndex) {
        float reward;
        if (currentIndex > 0 && currentIndex <= (this.getEpisodeDO().getLastTimeStep().getT() + 1) ) {
            reward = this.getEpisodeDO().getTimeSteps().get(currentIndex - 1).getReward();
        } else {
            reward = 0f;
        }
        return reward;
    }

    private double calculateValue(int tdSteps, int currentIndex) {
        double value = getBootstrapValue(currentIndex, tdSteps);
        value = addValueFromReward(currentIndex, tdSteps, value);
        return value;
    }

    private double calculateEntropyValue(int tdSteps, int currentIndex) {
        double value = getBootstrapEntropyValue(currentIndex, tdSteps);
        value = addEntropyValueFromReward(currentIndex, tdSteps, value);
        return value;
    }

    private double getBootstrapEntropyValue(int currentIndex, int tdSteps) {
        int bootstrapIndex = currentIndex + tdSteps;
        double value = 0;
        if (this.getEpisodeDO().isHybrid() || isReanalyse()) {
            if (  bootstrapIndex < this.getEpisodeDO().getLastTime() + 1) {
                value = this.getEpisodeDO().getTimeSteps().get(bootstrapIndex).getRootEntropyValueFromInitialInference() * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
            }
        } else {
            if (bootstrapIndex < this.getEpisodeDO().getLastTime() + 1) {
                value = this.getEpisodeDO().getTimeSteps().get(bootstrapIndex).getRootEntropyValueTarget()  * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
            }
        }
        return value;
    }

    // TODO there should be a special discount parameter
    private double addEntropyValueFromReward(int currentIndex, int tdSteps, double value) {
        int bootstrapIndex = currentIndex + tdSteps;
        for (int i = currentIndex + 1; i < this.getEpisodeDO().getLastTime()  + 1 && i < bootstrapIndex; i++) {
            value += (double) this.getEpisodeDO().getTimeSteps().get(i).getEntropy() * Math.pow(this.discount, i - (double) currentIndex);
        }
        return value;
    }

    private double addValueFromReward(int currentIndex, int tdSteps, double value) {
        int bootstrapIndex = currentIndex + tdSteps;

        if (currentIndex > this.getEpisodeDO().getLastTimeWithAction()) {
            int i = this.getEpisodeDO().getLastTimeWithAction();
            if (i < 0 ||  i > this.getEpisodeDO().getLastTime()) {
                int k = 42;
            }
            value += (double) this.getEpisodeDO().getTimeSteps().get(i).getReward() * Math.pow(this.discount, i - (double) currentIndex) * getPerspective(i - currentIndex);
        } else {
            for (int i = currentIndex; i < this.getEpisodeDO().getLastTimeWithAction() + 1 && i < bootstrapIndex; i++) {
                value += (double) this.getEpisodeDO().getTimeSteps().get(i).getReward() * Math.pow(this.discount, i - (double) currentIndex) * getPerspective(i - currentIndex);
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
        if (this.getEpisodeDO().isHybrid() || isReanalyse()) {
            switch(config.getVTarget()) {
                case V_INFERENCE:
                    if (  bootstrapIndex < this.getEpisodeDO().getLastTimeWithAction() + 1) {
                        value = this.getEpisodeDO().getTimeSteps().get(bootstrapIndex).getRootValueFromInitialInference() * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
                    }
                    break;
                case V_CONSISTENT:
                    if (bootstrapIndex < this.getEpisodeDO().getLastTimeWithAction() + 1) {
                        value = this.getEpisodeDO().getTimeSteps().get(bootstrapIndex).getRootValueTarget() * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
                    }
                    break;
                case V_MIX:
                    if (bootstrapIndex < this.getEpisodeDO().getLastTimeWithAction() + 1) {
                        value = this.getEpisodeDO().getTimeSteps().get(bootstrapIndex).getVMix() * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
                    }
                    break;
            }
        } else {
            if (bootstrapIndex < this.getEpisodeDO().getLastTimeWithAction() + 1) {
                value = this.getEpisodeDO().getTimeSteps().get(bootstrapIndex).getRootValueTarget() * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
            }
        }
        return value;
    }

    public abstract Player toPlay();

    public abstract String render();

    public abstract ObservationModelInput getObservationModelInput(int gamePosision);

    public ObservationModelInput getObservationModelInput() {
        return this.getObservationModelInput(this.getEpisodeDO().getLastTimeWithAction() + 1);
    }

    public void addObservationFromEnvironment() {
        this.getEpisodeDO().getLastTimeStep().setObservation(environment.getObservation());
    }

    public void addLegalActionFromEnvironment() {
        List<Action> actions = environment.getLegalActions();
        boolean[] result = new boolean[actionSpaceSize];
        for (Action action : actions) {
            result[action.getIndex()] = true;
        }
        this.getEpisodeDO().getLastTimeStep().setLegalActions(result);
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

    public void beforeReplayWithoutChangingActionHistory() {  // TODO check
        this.originalEpisodeDO = this.episodeDO;
        this.episodeDO = this.episodeDO.copyWithoutTimeSteps();

        int tend = this.originalEpisodeDO.getLastTimeWithAction();
        IntStream.range(0, tend + 1).forEach(i -> {
            this.episodeDO.getTimeSteps().add(this.originalEpisodeDO.getTimeSteps().get(i).copyPolicyTargetAndObservation());
        });
        this.episodeDO.addNewTimeStepDO();
        int t = this.episodeDO.getLastTime();
        this.episodeDO.getTimeSteps().get(t).setObservation(this.originalEpisodeDO.getTimeSteps().get(t).getObservation());

    }


    public void afterReplay() {
        this.setOriginalEpisodeDO(null);
    }

    public abstract void connectToEnvironment();

    public void initSearchManager(double pRandomActionRawAverage) {
        searchManager = new GumbelSearch(config, this, debug, pRandomActionRawAverage);
    }

    public double getPRatioMax() {
        int t = getEpisodeDO().getLastTimeWithAction() + 1;
        int tStart = (int) getEpisodeDO().getTHybrid();
        if (tStart >= t) return 1d;
        double[] pRatios = new double[t - tStart];
        IntStream.range(tStart, t).forEach(i -> {
            TimeStepDO timeStepDO = getEpisodeDO().getTimeSteps().get(i);
            if (timeStepDO.getAction() == null) {
                int k = 42;
            }
            int a = timeStepDO.getAction() ;
            if (getEpisodeDO().getTimeSteps().get(0).getPlayoutPolicy() == null) {
                pRatios[i - tStart] = 1;
            } else {
                pRatios[i - tStart] = timeStepDO.getPolicyTarget()[a] / timeStepDO.getPlayoutPolicy()[a];
            }
        });
        return getProductPathMax(pRatios);
    }


    public boolean isItExplorationTime() {
        return isItExplorationTime(getEpisodeDO().getLastTime() + 1);
    }

    public boolean isItExplorationTime(int t) {
        return t < getEpisodeDO().getTHybrid();
    }

    public boolean deepEquals(Game game) {
        return getEpisodeDO().deepEquals(game.getEpisodeDO());
    }

    public int findNewTReanalyseMin() {

        this.tReanalyseMin = (int)Math.max(getEpisodeDO().getTHybrid(), this.tReanalyseMin);

        int tMaxHorizon = getEpisodeDO().getLastTime();

        double localPRatioMax = 1;

        for (int t = tMaxHorizon; t >= this.tReanalyseMin; t--) {

            double pBase = 1;
            for (int i = this.tReanalyseMin; i < t; i++) {
                TimeStepDO timeStepDO = this.getEpisodeDO().getTimeSteps().get(i);
                pBase *= timeStepDO.getPlayoutPolicy()[timeStepDO.getAction()];
            }
            double p = 1;
            for (int i = this.tReanalyseMin; i < t; i++) {
                TimeStepDO timeStepDO = this.getEpisodeDO().getTimeSteps().get(i);
                p *= timeStepDO.getPolicyTarget() [timeStepDO.getAction()];
            }
            double pRatio = p / pBase;
            log.info("pRatio = %", pRatio);
            if (pRatio < this.config.getKMinLimit() * localPRatioMax) {
                this.tReanalyseMin = t+1;
                break;
            }
        }
        log.info("newTReanalyseMin = %", this.tReanalyseMin);
        return this.tReanalyseMin;
    }


    public  float getReward() {
         TimeStepDO  timeStepDO = this.episodeDO.getLastTimeStepWithAction();
        if (timeStepDO == null) return 0f;
        return timeStepDO.getReward();
    }
}
