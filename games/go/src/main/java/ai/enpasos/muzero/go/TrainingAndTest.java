package ai.enpasos.muzero.go;

import ai.enpasos.muzero.go.config.GoConfigFactory;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;

import static ai.enpasos.muzero.platform.MuZero.train;

@Slf4j
public class TrainingAndTest {

    public static void main(String[] args) {
        int size = 9;

        MuZeroConfig config = GoConfigFactory.getGoInstance(size);
        String dir = "./memory/go" + size + "/";
        config.setOutputDir(dir);

//        FileUtils.deleteDirectory(new File(dir));

        boolean freshBuffer = false;
        int numberOfEpochs = 1;
        train(config, freshBuffer, numberOfEpochs, false, false);

    }


}
