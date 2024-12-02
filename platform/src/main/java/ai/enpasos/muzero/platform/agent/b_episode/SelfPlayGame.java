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
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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




    public void uOkAnalyseGame(Game game,  boolean allTimestepsFromAStartingOne, int unrollSteps, boolean analyseAll ) {
        log.trace("uOkAnalyseGame");
        int tMax = game.getEpisodeDO().getLastTime();
        for (int tFrom = 0; tFrom <= tMax; tFrom++) {
            if ( game.getEpisodeDO().getTimeStep(tFrom).isToBeAnalysed() || analyseAll) {
                AnalyseResultFromOneTime r = analyseFromOneTime(game, tFrom, allTimestepsFromAStartingOne, unrollSteps);
                int uOk = r.getUOk();
               // updateUOk(game.getEpisodeDO(), tFrom, uOk);
                TimeStepDO ts = game.getEpisodeDO().getTimeStep(tFrom);
                if (ts.getUOk() != uOk) {
                    ts.setUOk(uOk);
                    ts.setUOkChanged(true);
                }
                ts.setNormedSampleError((float)r.getNormedSampleError());
            }
        }
    }

    // unrollSteps only relevant if allTimesteps is false
    private AnalyseResultFromOneTime analyseFromOneTime(Game game,   int tFrom, boolean allTimesteps, int unrollSteps) {
        EpisodeDO episode = game.getEpisodeDO();
        boolean onlyTestWhileOk = allTimesteps;

        AnalyseResultFromOneTime result = new AnalyseResultFromOneTime();
        int uOk = -1;
        int tMax =  episode.getLastTime();
        if (!allTimesteps) {
            tMax = Math.min(tMax, tFrom + unrollSteps);
        }
        int n = tMax - tFrom + 1;
        double normedSampleError = 0d;
        NDArray[] hiddenState = null;
        NetworkIO networkOutput;

        for (int t = tFrom; t <= tMax; t++) {

            if (t == tFrom) {
                game.setObservationInputTime(t);
                networkOutput = modelService.initialInference(game).join();
            } else {
                networkOutput = modelService.recurrentInference(hiddenState, episode.getAction(t-1)).join();
                closeHiddenState(hiddenState);
            }
            hiddenState = networkOutput.getHiddenState();
            normedSampleError = Math.max(normedSampleError, calculateNormedSampleError(episode, networkOutput, t, t != tFrom));

            if (normedSampleError > 1d ) {
                if (result.getUOk() == null) {
                    result.setUOk(t - tFrom - 1);
                }
                result.setNormedSampleError(normedSampleError);
                if (onlyTestWhileOk) {
                    closeHiddenState(hiddenState);
                    return result;
                }
            }
        }
        closeHiddenState(hiddenState);
        if (result.getUOk() == null) {
            result.setUOk(tMax - tFrom);
        }
        result.setNormedSampleError(normedSampleError);
        return result;
    }

    private void closeHiddenState(NDArray[] hiddenState) {
        for(NDArray ndArray : hiddenState) {
            ndArray.close();
        }
    }



    private double calculateNormedSampleError(EpisodeDO episode, NetworkIO networkOutput, int t, boolean withReward) {
        TimeStepDO timeStep = episode.getTimeStep(t);

       // NormedSampleError normedSampleError = new NormedSampleError();
      //  double normedSampleError  = 0d;

        float[] p = networkOutput.getPLegalValues();
        var pLabel = timeStep.getLegalact().getLegalActions();
        MyBCELoss myBCELoss = new MyBCELoss("MyBCELoss",1f / this.config.getActionSpaceSize(), 1, config.getLegalActionLossMaxThreshold(), 1);

        double normedSampleError  =   myBCELoss.maxNormedLoss(b2d(pLabel), f2d(p));
        //boolean ok = myBCELoss.isOk(b2d(pLabel), f2d(p));



        if (withReward) {
            var rLabel = t > 0 ? episode.getTimeStep(t - 1).getReward() : 0;
            double r = networkOutput.getReward();
            MyL2Loss myL2Loss = new MyL2Loss("MyL2Loss", config.getValueLossWeight() , config.getRewardLossThreshold(), 1);
            normedSampleError = Math.max(normedSampleError, myL2Loss.maxNormedLoss(rLabel, r));
           // ok = ok && myL2Loss.isOk(rLabel, r);
        }

        log.trace("t: {}, ok: {}",   t, normedSampleError <= 1d);
        return normedSampleError;
    }



//    private void updateUOk(EpisodeDO episode, int t, int uOK) {
//        TimeStepDO ts = episode.getTimeStep(t);
//        if (ts.getUOk() != uOK) {
//            ts.setUOk(uOK);
//            ts.setUOkChanged(true);
//        }
//    }




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
