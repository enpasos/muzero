package ai.enpasos.muzero.go.debug;

import ai.enpasos.muzero.go.config.ConfigFactory;
import ai.enpasos.muzero.platform.MuZeroConfig;

import static ai.enpasos.muzero.platform.debug.LossExtractor.listLossesForTrainedNetworks;

public class LossExtractor {

    public static void main(String[] args) throws Exception {
        MuZeroConfig config = ConfigFactory.getGoInstance(5);
        config.setOutputDir("./memory/");
        listLossesForTrainedNetworks(config);
    }

}
