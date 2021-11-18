package ai.enpasos.muzero.go;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.go.config.GoConfigFactory;
import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;

import static ai.enpasos.muzero.platform.MuZero.train;

@Slf4j
public class TrainingAndTest {

    public static void main(String[] args) throws URISyntaxException, IOException {
        int size = 9;

        MuZeroConfig config = GoConfigFactory.getGoInstance(size);
        String dir = "./memory/go"+ size + "/";
        config.setOutputDir(dir);

//        FileUtils.deleteDirectory(new File(dir));

        boolean freshBuffer = false;
        int numberOfEpochs = 1;
        train(config, freshBuffer, numberOfEpochs);

    }


}
