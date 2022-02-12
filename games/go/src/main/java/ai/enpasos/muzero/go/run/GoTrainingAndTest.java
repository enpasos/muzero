package ai.enpasos.muzero.go.run;


import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.run.MuZero;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;


@Slf4j
@Component
public class GoTrainingAndTest {

    @Autowired
    private MuZeroConfig config;


    @Autowired
    private MuZero muZero;

    public void run() {

         rmDir(config.getOutputDir());


      //  muZero.train(false, 1, false, false);
      //  muZero.train(true, 1);
        muZero.train(false, 1);


    }


}
