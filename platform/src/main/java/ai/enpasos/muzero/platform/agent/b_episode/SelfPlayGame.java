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


        int count = 1;
        while ((!untilEnd && count == 1)
                || (untilEnd && !game.isDone(
                playParameters.isReplay() || playParameters.isJustReplayWithInitialReference())
        )) {
            if (playParameters.isJustReplayWithInitialReference()) {
                playAction.justReplayActionWithInitialInference(game);
            } else {
                Action action = playAction.planAction(
                        game,
                        render,
                        fastRulesLearning,
                        justInitialInferencePolicy,
                        playParameters.getPRandomActionRawAverage(),
                        playParameters.replay,
                        playParameters.withGumbel);   // check parameter withRandomActions
                if (playParameters.isReplay()) {
                    game.pseudoApplyFromOriginalGame(action);
                } else {
                    game.apply(action);
                }
            }
            count++;
        }

        if (playParameters.isJustReplayWithInitialReference()) {
            playAction.justReplayActionWithInitialInference(game);
        }
        if (untilEnd) game.setEnvironment(null);

    }


}
