package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Slf4j
@Component
public class EpochHelper {

    @Autowired
    MuZeroConfig config;

    public int getEpoch() {
        int epoch = 0;
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            if (model.getBlock() == null) {
                MuZeroBlock block = new MuZeroBlock(config);
                model.setBlock(block);
                try {
                    model.load(Paths.get(config.getNetworkBaseDir()));
                } catch (Exception e) {
                    log.info("*** no existing model has been found ***");
                }
            }
            epoch = EpochHelper.getEpochFromModel(model);
        }
        return epoch;
    }
    public static int getEpochFromModel(Model model) {
        int epoch = 0;
        String prop = model.getProperty("Epoch");
        if (prop != null) {
            epoch = Integer.parseInt(prop);
        }
        return epoch;
    }
}
