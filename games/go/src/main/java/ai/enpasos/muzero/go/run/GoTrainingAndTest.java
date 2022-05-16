package ai.enpasos.muzero.go.run;


import ai.djl.Model;
import ai.djl.util.Pair;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.train.MuZero;
import ai.enpasos.muzero.platform.run.train.TrainParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;


@Slf4j
@Component
public class GoTrainingAndTest {

    @Autowired
    GoStartValueExtractor goStartValueExtractor;
    @Autowired
    ReplayBuffer replayBuffer;
    @Autowired
    private MuZeroConfig config;
    @Autowired
    private MuZero muZero;
    @Autowired
    private GoSurprise goSurprise;

    public void run() {
      //   rmDir(config.getOutputDir());

        muZero.train(TrainParams.builder()
           // .afterTrainingHookIn(this::adjustKomi)
                        .withoutFill(true)
          //  .afterSelfPlayHookIn((epoch, network) -> goSurprise.train(epoch, network))
            .build());
    }


}
