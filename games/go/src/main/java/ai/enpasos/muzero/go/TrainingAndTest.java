package ai.enpasos.muzero.go;

import ai.enpasos.muzero.go.config.GoConfigFactory;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static ai.enpasos.muzero.platform.MuZero.train;

@Slf4j
public class TrainingAndTest {

    public static void main(String[] args) throws IOException {
        int size = 5;

        MuZeroConfig config = GoConfigFactory.getGoInstance(size);
        String dir = "./memory/go" + size + "/";
        config.setOutputDir(dir);

        FileUtils.deleteDirectory(new File(dir));

        boolean freshBuffer = false;
        int numberOfEpochs = 1;
        train(config, freshBuffer, numberOfEpochs, false, true);


    }


}
