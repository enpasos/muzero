package ai.enpasos.muzero.tictactoe.debug;

import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.debug.LossExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TicTacToeLossExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    LossExtractor lossExtractor;

    public   void run()   {
      //  config.setOutputDir("./memory/tictactoe/");
        lossExtractor.listLossesForTrainedNetworks();
    }

}
