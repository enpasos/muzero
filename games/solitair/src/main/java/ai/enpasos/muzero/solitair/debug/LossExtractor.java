package ai.enpasos.muzero.solitair.debug;

import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.solitair.config.SolitairConfigFactory;

import static ai.enpasos.muzero.platform.debug.LossExtractor.listLossesForTrainedNetworks;

public class LossExtractor {

    public static void main(String[] args) throws Exception {
        MuZeroConfig config = SolitairConfigFactory.getSolitairInstance();
        config.setOutputDir("./memory/");
        listLossesForTrainedNetworks(config);
    }

}
