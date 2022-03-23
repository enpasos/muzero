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
public class GoTrainingAndTest2 {

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

        rmDir(config.getOutputDir());

        muZero.train2(TrainParams.builder()
            .afterTrainingHookIn(this::adjustKomi)
            .afterSelfPlayHookIn((epoch, network) -> goSurprise.train(epoch, network))
            .build());
    }

    private void adjustKomi(Integer epoch, Model model) {
        if (epoch < 20) return;
        List<Pair<Integer, Double>> pairList = goStartValueExtractor.smoothing(goStartValueExtractor.valuesForTrainedNetworks(), 10);
        Pair<Integer, Double> pair = pairList.get(pairList.size() - 1);
        double v = pair.getValue();
        double oldKomi = config.getKomi();
        double newKomi = oldKomi;
        if (v > 0.1 && config.getKomi() < config.getMaxKomi()) {
            newKomi += 1d;
        } else if (v < -0.1) {
            newKomi -= 1d;
        }
        if (newKomi != oldKomi) {
            config.setKomi(newKomi);
            log.info("komi changed: " + oldKomi + " -> " + newKomi);
            model.setProperty("komi", newKomi + "");
            this.replayBuffer.rebuildGames();
        }
    }


}
