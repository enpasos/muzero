package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.train.MuZeroFast2;
import ai.enpasos.muzero.platform.run.train.TrainParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;


@Slf4j
@Component
public class TicTacToeTrainFastAndTest {

    @Autowired
    private MuZeroFast2 muZeroFast;

    @Autowired
    private MuZeroConfig config;

    @SuppressWarnings("squid:S125")
    public void run() {
        rmDir(config.getOutputDir());

        muZeroFast.train(TrainParams.builder()
            // .withoutFill(true)
            .build());
    }


}
