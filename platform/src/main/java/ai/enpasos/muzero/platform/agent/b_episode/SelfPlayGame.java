package ai.enpasos.muzero.platform.agent.b_episode;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.c_planning.PlanAction;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SelfPlayGame {

    @Autowired
    MuZeroConfig config;

    @Autowired
    PlanAction playAction;

    @Autowired
    GameBuffer replayBuffer;


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
//                    if (playParameters.hybrid2) {
//                       game.hybrid2ApplyAction(action);
//                    } else {
                       game.apply(action);



                       // TODO: switch??






                  //  }
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
