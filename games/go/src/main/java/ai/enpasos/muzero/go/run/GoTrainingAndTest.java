package ai.enpasos.muzero.go.run;


import ai.djl.util.Pair;
import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import ai.enpasos.muzero.platform.run.train.MuZero;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.train.TrainParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


@Slf4j
@Component
public class GoTrainingAndTest {

    @Autowired
    private MuZeroConfig config;


    @Autowired
    GoStartValueExtractor goStartValueExtractor;

    @Autowired
    private MuZero muZero;

    public void run() {

     //    rmDir(config.getOutputDir());


      //  muZero.train(false, 1, false, false);
      //  muZero.train(true, 1);
       // muZero.train(false, 1);

    muZero.train(TrainParams.builder()
        .hookIn((epoch, model) -> {
            if (epoch < 40) return;
            List<Pair<Integer, Double>> pairList = goStartValueExtractor.smoothing(goStartValueExtractor.valuesForTrainedNetworks(), 10);
            Pair<Integer, Double> pair = pairList.get(pairList.size()-1);
            double v = pair.getValue();
            double oldKomi = config.getKomi();
            String komiStrFromModel = model.getProperty("komi");
            if(komiStrFromModel != null) {
                try {
                    oldKomi = Double.parseDouble(komiStrFromModel);
                } catch (Exception e) {
                    log.info("old komi could not be retrieved from model therefore it is taken from configuration");
                }
            }
            double newKomi = oldKomi;
            if (v > 0.1) {
                newKomi +=   1d;
            } else if (v < -0.1) {
                newKomi -=   1d;
            }
            if (newKomi != oldKomi) {
                config.setKomi(newKomi);
                log.info("komi changed: " + oldKomi + " -> " + newKomi);
                model.setProperty("komi", newKomi + "");
            }
        })
        .build());
    }


}
