package ai.enpasos.muzero.tictactoe.run;

import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.LossExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TicTacToeLossExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    LossExtractor lossExtractor;


    public void run() {
        lossExtractor.listLossesForTrainedNetworks(1);
    }

}
