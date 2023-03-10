package ai.enpasos.muzero.pegsolitair.run;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.TrainParams;
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
    private MuZeroLoop muZero;

    public void run() {

        rmDir(config.getOutputDir());

        try {
            muZero.train(TrainParams.builder()
                .render(true)
                .build());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
