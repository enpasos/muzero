package ai.enpasos.muzero.go.run;

import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.TrainParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;


@Slf4j
@Component
public class GoTrainingAndTest {

    @Autowired
    GoStartValueExtractor goStartValueExtractor;
    @Autowired
    GameBuffer gameBuffer;
    @Autowired
    private MuZeroConfig config;
    @Autowired
    private MuZeroLoop muZero;


    @SuppressWarnings({"squid:S125", "java:S2583", "java:S2589"})
    public void run() {
        boolean startFromScratch = false;

        if (startFromScratch) {
            rmDir(config.getOutputDir());
        }


        try {
            muZero.train(TrainParams.builder()
                .render(true)

                .build());
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.interrupted();
        } catch (ExecutionException e) {
            throw new MuZeroException(e);
        }

    }

}
