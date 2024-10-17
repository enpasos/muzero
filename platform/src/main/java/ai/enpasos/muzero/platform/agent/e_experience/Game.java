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
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.common.Functions.b2f;
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


    protected Game(@NotNull MuZeroConfig config) {
        this.config = config;
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
        return     replay && getOriginalEpisodeDO().getLastTimeWithAction() == getEpisodeDO().getLastTimeWithAction()
                 || !replay && getEpisodeDO().getLastTimeWithAction() + 1 >= config.getMaxMoves()
                || this.getEnvironment()!= null && !replay && terminal();
            //    || this.isHybrid2() &&  getOriginalEpisodeDO().getLastTimeWithAction() == getEpisodeDO().getLastTimeWithAction();
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

    // TODO simplify Action handling ... just int not Marker Interface
    public List<Action> legalActions() {
        boolean[] b = this.episodeDO.getLegalActionsFromLatestTimeStepWithoutAction();

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
        TimeStepDO ts = this.getEpisodeDO().getLastTimeStep();
        ts.setReward(reward);
        ts.setAction(action.getIndex());

        // now ... observation and legal actions already belong to the next timestamp
        getEpisodeDO().addNewTimeStepDO();
        addObservationFromEnvironment();
        addLegalActionFromEnvironment();
        setActionApplied(true);
    }

    public void justRemoveLastAction(Action action) {
        this.getEpisodeDO().removeTheLastAction();
  setActionApplied(true);
    }


//    public void hybrid2ApplyAction(Action action) {
//        apply(action);
//    }

    public List<Target> makeTarget(int stateIndex, int numUnrollSteps) {
        List<Target> targets = new ArrayList<>();

        IntStream.range(stateIndex, stateIndex + numUnrollSteps + 1).forEach(currentIndex -> {
            Target target = new Target();
            fillTarget(currentIndex, target );
            targets.add(target);
        });
        return targets;
    }


    private float getReward(int currentIndex) {
        // perspective is missing
        float reward;
        if (currentIndex > 0 && currentIndex <= this.episodeDO.getLastTime()) {
            reward = episodeDO.getTimeStep(currentIndex - 1).getReward();
        } else {
            reward = 0f;
        }
        return reward;
    }


    @SuppressWarnings("java:S3776")
    private void fillTarget(int currentIndex, Target target) {

        int tmax = this.getEpisodeDO().getLastTime();


        int   tdSteps = getTdSteps( currentIndex);
        double   value = calculateValue(tdSteps, currentIndex);
        float reward = getReward(currentIndex);

        if (currentIndex < this.getEpisodeDO().getLastTime() ) {
            TimeStepDO timeStepDO = this.getEpisodeDO().getTimeSteps().get(currentIndex);
            target.setLegalActions(b2f(timeStepDO.getLegalact().getLegalActions()));
            target.setValue((float) value);
            target.setReward(reward);
            float[] policy = timeStepDO.getPolicyTarget();
            target.setPolicy(policy);
        } else {

            target.setValue(0);

            target.setReward(reward);

            // 0f means not force to the policy (property of SoftmaxCrossEntropyLoss)
            target.setPolicy(new float[this.actionSpaceSize]);
            Arrays.fill(target.getPolicy(), 0f);

            // 0f means a force here
            float[]legalActions = new float[this.actionSpaceSize];
            Arrays.fill(legalActions, 0f);
            target.setLegalActions(legalActions);
        }

    }

    private int getTdSteps(int currentIndex) {
        int tdSteps;
        if (this.reanalyse) {
            throw new MuZeroException("check code");
//            if (episodeDO.isHybrid() && isItExplorationTime(currentIndex)) {
//                tdSteps = 0;
//            } else {
//                int tMaxHorizon = episodeDO.getLastTime() - 1;
//                tdSteps = getTdSteps(currentIndex, tMaxHorizon);
//            }
        } else {
            if (episodeDO.isHybrid() && isItExplorationTime(currentIndex)) {
                tdSteps = 0;
            } else {
                tdSteps = episodeDO.getTdSteps();
            }
        }
        return tdSteps;
    }


    private double calculateValue(int tdSteps, int currentIndex) {
        double value = getBootstrapValue(currentIndex, tdSteps);
        value = addValueFromReward(currentIndex, tdSteps, value);
        return value;
    }

    private double addValueFromReward(int currentIndex, int tdSteps, double value) {
        int bootstrapIndex = currentIndex + tdSteps;

        if (currentIndex > this.getEpisodeDO().getLastTimeWithAction()) {
            int i = this.getEpisodeDO().getLastTimeWithAction();
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
        if (this.getEpisodeDO().isHybrid() || isReanalyse()  ) {
            switch(config.getVTarget()) {
                case V_INFERENCE:  // the default case ... remove the others
                    if (  bootstrapIndex <= this.getEpisodeDO().getLastTime() ) {
                        value = this.getEpisodeDO().getTimeSteps().get(bootstrapIndex).getRootValueFromInitialInference() * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
                    }
                    break;
                case V_CONSISTENT:
                    if (bootstrapIndex <= this.getEpisodeDO().getLastTime() ) {
                        value = this.getEpisodeDO().getTimeSteps().get(bootstrapIndex).getRootValueTarget() * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
                    }
                    break;
                case V_MIX:
//                    if (bootstrapIndex <= this.getEpisodeDO().getLastTime()) {
//                        value = this.getEpisodeDO().getTimeSteps().get(bootstrapIndex).getVMix() * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
//                    }
                    throw new MuZeroException("V_MIX not implemented");

            }
        } else {
            if (bootstrapIndex <= this.getEpisodeDO().getLastTime() ) {
                value = this.getEpisodeDO().getTimeSteps().get(bootstrapIndex).getRootValueTarget() * Math.pow(this.discount, tdSteps) * getPerspective(tdSteps);
            }
        }
        return value;
    }

    public abstract Player toPlay();

    public abstract String render();

    public abstract ObservationModelInput getObservationModelInput(int inputTime);


    private int observationInputTime = -1;

    public ObservationModelInput getObservationModelInput() {
        int t = observationInputTime;
        if (t == -1) {
            t = this.getEpisodeDO().getLastTimeWithAction() + 1;
        }
        return this.getObservationModelInput(t);
    }

    public Action getAction() {
        int t = observationInputTime - 1;
        if (this.getEpisodeDO().getActions().size() == 0)   {
            return null;
        }
        if (t < 0 ) {
            t = this.getEpisodeDO().getLastTimeWithAction();
        }
        return config.newAction(this.getEpisodeDO().getTimeStep(t).getAction());
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
        this.getEpisodeDO().getLastTimeStep().addLegalActions( result);
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
            this.episodeDO.getTimeSteps().add(this.originalEpisodeDO.getTimeStep(i).copyPolicyTargetAndObservation());
        });
        this.episodeDO.addNewTimeStepDO();
        int t = this.episodeDO.getLastTime();
        this.episodeDO.getTimeStep(t).setObservation(this.originalEpisodeDO.getTimeStep(t).getObservation());
        this.episodeDO.getTimeStep(t).setId(this.originalEpisodeDO.getTimeStep(t).getId());

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
        int tStart = (int) getEpisodeDO().getTStartNormal();
        if (tStart >= t) return 1d;
        double[] pRatios = new double[t - tStart];
        IntStream.range(tStart, t).forEach(i -> {
            TimeStepDO timeStepDO = getEpisodeDO().getTimeSteps().get(i);
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
        return isItExplorationTime(getEpisodeDO().getLastTime() );
    }

    public boolean isItExplorationTime(int t) {
        return t < getEpisodeDO().getTStartNormal();
    }

    public boolean deepEquals(Game game) {
        return getEpisodeDO().deepEquals(game.getEpisodeDO());
    }




    public  float getReward() {
         TimeStepDO  timeStepDO = this.episodeDO.getLastTimeStepWithAction();
        if (timeStepDO == null) return 0f;
        return timeStepDO.getReward();
    }

    public void resetAllOriginalActions() {
        int n = this.originalEpisodeDO.getTimeSteps().size();
        IntStream.range(0,n).forEach(t -> {
            this.episodeDO.getTimeStep(t).setAction(this.originalEpisodeDO.getTimeStep(t).getAction());
        });
    }

    // TODO: performance optimize in storing intermediate results
    public void calculateK() {
        /// int i = 42;

        int tmax = episodeDO.getLastTime() - 1;

        int t0 = episodeDO.getLastTimeWithAction() + 1;


        // double localPRatioMax = Math.min(this.pRatioMax, config.getOffPolicyRatioLimit());

        double pBase = 1;
        for (int t = t0; t <= tmax; t++) {
            TimeStepDO timeStepDO = this.getEpisodeDO().getTimeSteps().get(t);
            TimeStepDO originalTimeStepDO = this.getOriginalEpisodeDO().getTimeSteps().get(t);
            pBase *= timeStepDO.getPlayoutPolicy()[originalTimeStepDO.getAction()];
        }
        double p = 1;
        for (int t = t0; t <= tmax; t++) {
            TimeStepDO timeStepDO = this.getEpisodeDO().getTimeSteps().get(t);
            TimeStepDO originalTimeStepDO = this.getOriginalEpisodeDO().getTimeSteps().get(t);
            p *= timeStepDO.getPolicyTarget()[originalTimeStepDO.getAction()];
        }
        double pRatio = p / pBase;
        TimeStepDO timeStepDO = this.getEpisodeDO().getTimeSteps().get(t0);
        timeStepDO.setK(pRatio / config.getOffPolicyRatioLimit());

    }

    public int getFirstSamplePosition() {
        if (!this.isReanalyse()) {
            if (episodeDO.isHybrid())
            {
                return (int) episodeDO.getTStartNormal();
            } else {
                return 0;
            }

        } else {
            for (int t = 0; t < this.getEpisodeDO().getLastTimeWithAction(); t++) {
                if (this.getEpisodeDO().getTimeStep(t).getK() != 0d) return t;
            }
            return 0;
        }
    }

    public void hybrid2ApplyAction(Action action) {
        float reward = this.environment.step(action);
        this.getEpisodeDO().getLastTimeStep().setReward(reward);
        this.getEpisodeDO().getLastTimeStep().setAction(action.getIndex());

        // now ... observation and legal actions already belong to the next timestamp
        getEpisodeDO().addNewTimeStepDO();
        addObservationFromEnvironment();
        addLegalActionFromEnvironment();
        setActionApplied(true);
    }
}
