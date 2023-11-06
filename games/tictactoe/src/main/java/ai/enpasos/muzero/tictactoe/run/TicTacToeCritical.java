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
        int epoch = 348;

       // int[] actions = {1,7,3,6,8,2,4,0,5};
       // int[] actions = {1,7,3};
        int[] actions = {1, 0, 3, 2, 6};
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


//
//        List<Integer> actionList = new ArrayList<>();
//
//        games.stream().forEach(g -> {
//            TimeStepDO ts = g.getEpisodeDO().getLastTimeStepWithAction();
//            g.setObservationInputTime(ts.getT());
//            actionList.add(ts.getAction());
//        });
//
//
//        modelService.loadLatestModel(epoch).join();
//        List<NetworkIO> results = modelService.initialInference(games).join();
//
//        modelService.recurrentInference(ga
//        infResult = modelService.recurrentInference(s, action).join();

      //  NDArray s = results.get(0).getHiddenState();


    }


//    private double[][] getInMindValues(  int[] actions, int extra, int actionspace) {
//        double[][] values = new double[actions.length + 1][actions.length + 1 + extra];
//        Game game = config.newGame(true,true);
//        for (int t = 0; t <= actions.length; t++) {
//            NetworkIO infResult = modelService.initialInference(game).join();
//            NDArray s = infResult.getHiddenState();
//            values[actions.length][t] = infResult.getValue();
//            System.arraycopy(values[actions.length], 0, values[t], 0, t + 1);
//            for (int r = t; r < actions.length + extra; r++) {
//                int action;
//                if (r < actions.length) {
//                    action = actions[r];
//                } else {
//                    action = ThreadLocalRandom.current().nextInt(actionspace);
//                }
//                infResult = modelService.recurrentInference(s, action).join();
//                s = infResult.getHiddenState();
//                values[t][r + 1] = infResult.getValue();
//            }
//            if (t < actions.length) game.apply(actions[t]);
//        }
//        return values;
 //   }
}
