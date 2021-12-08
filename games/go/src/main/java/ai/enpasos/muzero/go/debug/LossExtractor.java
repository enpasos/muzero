package ai.enpasos.muzero.go.debug;

import ai.enpasos.muzero.go.config.GoConfigFactory;
import ai.enpasos.muzero.platform.config.MuZeroConfig;

import static ai.enpasos.muzero.platform.debug.LossExtractor.listLossesForTrainedNetworks;

public class LossExtractor {

    public static void main(String[] args) throws Exception {
        int size = 5;
        MuZeroConfig config = GoConfigFactory.getGoInstance(size);
        config.setOutputDir("./memory/go"+ size + "/");
        listLossesForTrainedNetworks(config);
    }

}
