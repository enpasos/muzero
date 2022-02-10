package ai.enpasos.muzero.pegsolitair.run;

import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.LossExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PegSolitairLossExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    LossExtractor lossExtractor;


    @SuppressWarnings("squid:S125")
    public void run() {
        //  config.setOutputDir("./memory/pegsolitair/");
        lossExtractor.listLossesForTrainedNetworks();
    }

}
