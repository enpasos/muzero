package ai.enpasos.muzero.pegsolitair.run;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.train.MuZero;
import ai.enpasos.muzero.platform.run.train.TrainParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;


@Slf4j
@Component
public class PegSolitairTrainingAndTest {

    @Autowired
    private MuZeroConfig config;


    @Autowired
    private MuZero muZero;

    public void run() {

        rmDir(config.getOutputDir());

        muZero.train(TrainParams.builder()
            .render(true)
            .build());

    }


}
