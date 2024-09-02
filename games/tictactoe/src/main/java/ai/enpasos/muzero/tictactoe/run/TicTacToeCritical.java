package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static ai.enpasos.muzero.platform.config.PlayTypeKey.PLAYOUT;

@Slf4j
@Component
public class TicTacToeCritical {

    @Autowired
    Inference inference;


    @Autowired
    ModelService modelService;

    @Autowired
    MuZeroConfig config;

    public void run() {
        int epoch = 202;


        int[] actions = {0,1,2,6,3, 7, 4, 8};
        Game game = this.config.newGame(true, true);
        game.apply(actions);
        List<Game> games  = List.of(game);

        PlayTypeKey originalPlayTypeKey = config.getPlayTypeKey();
        config.setPlayTypeKey(PLAYOUT);

        try {
            modelService.loadLatestModel(epoch).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new MuZeroException(e);
        }

        int[] nextActions = inference.aiDecisionForGames(games, true, epoch );

        log.info("nextAction: {}", nextActions[0]);



    }



}
