package ai.enpasos.muzero.platform.agent.b_episode;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.c_planning.PlanAction;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyBCELoss;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyL2Loss;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static ai.enpasos.muzero.platform.common.Functions.*;

@Component
@Slf4j
public class SelfPlayGame {

    @Autowired
    MuZeroConfig config;

    @Autowired
    PlanAction playAction;

    @Autowired
    GameBuffer replayBuffer;

    @Autowired
    ModelService modelService;




    public void uOkAnalyseGame(Game game, boolean allTimesteps ) {
        log.trace("uOkAnalyseGame");
        int tMax = game.getEpisodeDO().getLastTime();
        for (int tFrom = 0; tFrom <= tMax; tFrom++) {
            int uOk = analyseFromOneTime(game, tFrom, allTimesteps);
            updateUOk(game.getEpisodeDO(), tFrom, uOk);
        }
    }

    private int analyseFromOneTime(Game game,   int tFrom, boolean allTimesteps) {
        EpisodeDO episode = game.getEpisodeDO();
        //int tMax = Math.min(episode.getLastTime(), tFrom+unrollSteps);
        int tMax =  episode.getLastTime();
        if (!allTimesteps) {
            tMax = Math.min(episode.getLastTime(), tFrom + episode.getUnrollSteps());
        }
        NDArray[] hiddenState = null;
        NetworkIO networkOutput;
        int t = tFrom;
        for (; t <= tMax; t++) {
            game.setObservationInputTime(t);
            if (t == tFrom) {
                networkOutput = modelService.initialInference(game).join();
            } else {
                networkOutput = modelService.recurrentInference(hiddenState, episode.getAction(t-1)).join();
            }
            hiddenState = networkOutput.getHiddenState();
            boolean currentIsOk = checkOkStatus(episode, networkOutput, t);
            if (!currentIsOk) {
                return t-tFrom-1;
            }
        }
        return  tMax-tFrom;
    }



    private boolean checkOkStatus(EpisodeDO episode, NetworkIO networkOutput, int t) {
        TimeStepDO timeStep = episode.getTimeStep(t);
        double rewardLabel = t > 0 ? episode.getTimeStep(t - 1).getReward() : 0;
        boolean currentIsOk = isOk(networkOutput, timeStep.getLegalact().getLegalActions(), rewardLabel);
        log.trace("t: {}, ok: {}",   t, currentIsOk);
        return currentIsOk;
    }



    private void updateUOk(EpisodeDO episode, int t, int uOK) {
        TimeStepDO ts = episode.getTimeStep(t);
        if (ts.getUOk() != uOK) {
            ts.setUOk(uOK);
            ts.setUOkChanged(true);
        }
    }

    private boolean isOk(NetworkIO networkOutput, boolean[] pLabel, double rLabel) {
        double r = networkOutput.getReward();
        float[] p = networkOutput.getPLegalValues();

        MyBCELoss myBCELoss = new MyBCELoss("MyBCELoss",1f / this.config.getActionSpaceSize(), 1, config.getLegalActionLossMaxThreshold());
        MyL2Loss myL2Loss = new MyL2Loss("MyL2Loss", config.getValueLossWeight() , config.getRewardLossThreshold());
        return  myBCELoss.isOk(b2d(pLabel), f2d(p)) && myL2Loss.isOk(rLabel, r);
    }


    public void play(Game game, PlayParameters playParameters) {
        log.trace("play");
        boolean render = playParameters.isRender();
        boolean fastRulesLearning = playParameters.isFastRulesLearning();
        boolean untilEnd = playParameters.isUntilEnd();
        boolean justInitialInferencePolicy = playParameters.isJustInitialInferencePolicy();

        game.getEpisodeDO().setTdSteps(config.getTdSteps());


        if (playParameters.isJustReplayToGetRewardExpectations()) {
            for(int t = 0; t <= game.getEpisodeDO().getLastTimeWithAction(); t++) {

                game.setObservationInputTime(t);
                    playAction.justReplayActionToGetRulesExpectations(game);

            }
            game.setObservationInputTime(-1);

            return;
        }
        if (playParameters.isJustReplayToGetRewardExpectationsFromStart()) {

            int t = 0;
                game.setObservationInputTime(t);
                playAction.justReplayActionToGetRulesExpectationsFromStart(game);

         //   game.setObservationInputTime(-1);

            return;
        }


        int count = 1;
        while (untilEnd &&  playParameters.isReplay() ?

                playParameters.isReplay() && count <= game.getOriginalEpisodeDO().getLastTimeWithAction()+1
                :
                ((!untilEnd && count == 1)
                        || (untilEnd && !game.isDone(
                                playParameters.isJustReplayWithInitialReference()
                                    || playParameters.isJustReplayToGetRewardExpectations()
                )))) {
       //     log.info("LastTimeWithAction: " + game.getEpisodeDO().getLastTimeWithAction());
            if (playParameters.isJustReplayWithInitialReference()) {
                playAction.justReplayActionWithInitialInference(game);
            }  else {
                Action action = playAction.planAction(
                        game,
                        render,
                        fastRulesLearning,
                        justInitialInferencePolicy,
                        playParameters.getPRandomActionRawAverage(),
                    //    playParameters.isDrawNotMaxWhenJustWithInitialInference(),   // check parameter isDrawNotMaxWhenJustWithInitialInference()
                        playParameters.replay,
                        playParameters.withGumbel);   // check parameter withRandomActions
             //   Action action = playAction.planActionOld(game, render, fastRulesLearning, justInitialInferencePolicy, playParameters.getPRandomActionRawAverage(), playParameters.isDrawNotMaxWhenJustWithInitialInference());
                if (playParameters.isReplay()) {
                    game.calculateK();
                    game.justRemoveLastAction(action);

                } else {
                       game.apply(action);
                }
            }
            count++;
        }

        if (playParameters.isJustReplayWithInitialReference()) {
            playAction.justReplayActionWithInitialInference(game);
        } else if (playParameters.isReplay()) {
            // replay
//            List<Double> ks = new ArrayList<>();
//IntStream.range(0, game.getEpisodeDO().getLastTime()).forEach(t -> ks.add(game.getEpisodeDO().getTimeStep(t).getK()));
//    log.info("epoch {}, id {}, ks = {}", game.getEpisodeDO().getTrainingEpoch(), game.getEpisodeDO().getId(),    ks.toString());

            game.resetAllOriginalActions();
        }
        if (untilEnd) game.setEnvironment(null);

    }


}
