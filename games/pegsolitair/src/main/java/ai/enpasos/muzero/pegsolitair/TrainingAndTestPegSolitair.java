package ai.enpasos.muzero.pegsolitair;


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
public class TrainingAndTestPegSolitair {

    @Autowired
    private MuZeroConfig config;


    @Autowired
    private MuZero muZero;

    public void run()  {

//        String dir = "./memory/pegsolitair/";
//        config.setOutputDir(dir);

//        try {
//            FileUtils.deleteDirectory(new File(dir));
//        } catch (Exception e) {
//            throw new MuZeroException(e);
//        }


        muZero.train(false, 1, true, true);



    }



}
