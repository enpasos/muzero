package ai.enpasos.muzero.pegsolitair;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.pegsolitair.config.PegSolitairConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static ai.enpasos.muzero.platform.MuZero.train;

@Slf4j
public class TrainingAndTest {

    public static void main(String[] args) throws URISyntaxException, IOException {
        MuZeroConfig config = PegSolitairConfigFactory.getSolitairInstance();
        String dir = "./memory/";
        config.setOutputDir(dir);

        FileUtils.deleteDirectory(new File(dir));

        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            train(model, config);
        }
//        boolean passed = SolitairTest.test(config);
//        String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
//        log.info(message);
//        if (!passed) throw new RuntimeException(message);
    }


}
