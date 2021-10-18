package ai.enpasos.muzero.tictactoe.debug;

import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.tictactoe.config.ConfigFactory;

import static ai.enpasos.muzero.debug.LossExtractor.listLossesForTrainedNetworks;

public class LossExtractor {

    public static void main(String[] args) throws Exception {
        MuZeroConfig config = ConfigFactory.getTicTacToeInstance();
        config.setOutputDir("./memory/");
        listLossesForTrainedNetworks(config);
    }

}
