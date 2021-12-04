package ai.enpasos.muzero.pegsolitair.debug;

import ai.enpasos.muzero.pegsolitair.config.PegSolitairConfigFactory;
import ai.enpasos.muzero.platform.config.MuZeroConfig;

import static ai.enpasos.muzero.platform.debug.LossExtractor.listLossesForTrainedNetworks;

public class LossExtractor {

    public static void main(String[] args) throws Exception {
        MuZeroConfig config = PegSolitairConfigFactory.getSolitairInstance();
        config.setOutputDir("./memory/");
        listLossesForTrainedNetworks(config);
    }

}
