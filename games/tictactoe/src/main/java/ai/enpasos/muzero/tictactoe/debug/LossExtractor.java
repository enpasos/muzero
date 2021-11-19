package ai.enpasos.muzero.tictactoe.debug;

import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.tictactoe.config.TicTacToeConfigFactory;

import static ai.enpasos.muzero.platform.debug.LossExtractor.listLossesForTrainedNetworks;

public class LossExtractor {

    public static void main(String[] args) throws Exception {
        MuZeroConfig config = TicTacToeConfigFactory.getTicTacToeInstance();
        config.setOutputDir("./memory/");
        listLossesForTrainedNetworks(config);
    }

}
