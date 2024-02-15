package ai.enpasos.muzero.connect4.run;


import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.TrainParams;
import ai.enpasos.muzero.connect4.run.test.Connect4Test;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;


@Slf4j
@Component
public class Connect4PolicyOnly {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private Connect4Test ticTacToeTest;

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
