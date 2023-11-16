package ai.enpasos.muzero.tictactoe.run;


import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import ai.enpasos.muzero.platform.run.FillValueTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

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

        int[] nextActions = inference.aiDecisionForGames(games, true );

        log.info("nextAction: {}", nextActions[0]);



    }



}
