package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.TrainParams;
import ai.enpasos.muzero.tictactoe.run.test.TicTacToeTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;


@Slf4j
@Component
public class TicTacToePolicyOnly {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private TicTacToeTest ticTacToeTest;

    @Autowired
    private MuZeroLoop muZero;

    public void run() {

        try {
            muZero.train(TrainParams.builder()
                .render(true)
                .doNotLoadLatestState(true)
                .build());
        } catch (InterruptedException e) {
            throw new MuZeroException(e);
        } catch (ExecutionException e) {
            throw new MuZeroException(e);
        }


    }


}
