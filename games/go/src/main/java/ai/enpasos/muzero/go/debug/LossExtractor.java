package ai.enpasos.muzero.go.debug;

import ai.enpasos.muzero.go.config.GoConfigFactory;
import ai.enpasos.muzero.platform.config.MuZeroConfig;

import static ai.enpasos.muzero.platform.debug.LossExtractor.listLossesForTrainedNetworks;

public class LossExtractor {

    public static void main(String[] args) throws Exception {
        MuZeroConfig config = GoConfigFactory.getGoInstance(5);
        config.setOutputDir("./memory/");
        listLossesForTrainedNetworks(config);
    }

}
