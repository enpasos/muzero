package ai.enpasos.muzero.pegsolitair;

import ai.enpasos.muzero.platform.config.MuZeroConfig;
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

 //       FileUtils.deleteDirectory(new File(dir));

        boolean freshBuffer = false;
        int numberOfEpochs = 1;
        train(config, freshBuffer, numberOfEpochs);
    }


}
