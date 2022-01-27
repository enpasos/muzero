package ai.enpasos.muzero.go;


import ai.enpasos.muzero.platform.MuZero;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;


@Slf4j
@Component
public class GoTrainingAndTest {

    @Autowired
    private MuZeroConfig config;


    @Autowired
    private MuZero muZero;

    public void run() {

//        try {
//            FileUtils.deleteDirectory(new File(config.getOutputDir()));
//        } catch (Exception e) {
//            throw new MuZeroException(e);
//        }

        muZero.train(false, 1, false, false);
      //  muZero.train(true, 1);
       // muZero.train(false, 1);


    }


}
