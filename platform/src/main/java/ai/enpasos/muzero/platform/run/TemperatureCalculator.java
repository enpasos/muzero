package ai.enpasos.muzero.platform.run;


import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.NetworkIOService;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueRepo;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

@Slf4j
@Component
public class TemperatureCalculator {
    @Autowired
    NetworkIOService networkIOService;
    @Autowired
    ModelService modelService;
    @Autowired
    ValueRepo valueRepo;
    @Autowired
    EpisodeRepo episodeRepo;
    @Autowired
    TimestepRepo timestepRepo;
    @Autowired
    DBService dbService;
    @Autowired
    MuZeroConfig config;
    @Autowired
    GameProvider gameProvider;


    public void aggregatePerEpoch() {
        List<Integer> epochs = episodeRepo.findEpochs();
        DecimalFormat df = new DecimalFormat("#.###.###.##0,0000000");

        log.info("temperature aggregation for epochs {}", epochs);
        for (Integer epoch : epochs) {

            double temperature = aggregateOnEpoch(epoch);

            System.out.println(epoch + ";" + df.format(temperature));
        }
    }

    private double aggregateOnEpoch(Integer epoch) {
        List<ValueDO> valueDOs = valueRepo.findValuesForEpoch(epoch);
        double sum = 0d;
        long count = 0;
        for(int i = 0; i < valueDOs.size(); i++) {
            ValueDO valueDO = valueDOs.get(i);
            long c = valueDO.getCount();
            count += c;
            sum += c * valueDO.getValueHatSquaredMean();
        }
        return sum/count;
    }


    public void runOnTimeStepLevel() {
        int n = 10;
        List<Integer> epochs = episodeRepo.findEpochs();
        for (Integer epoch : epochs) {
            log.info("temperature calculating for epoch {}", epoch);
            runOnTimeStepLevel(epoch, n);
        }
    }


    public void runOnTimeStepLevel(int epoch, int n) {
        List<TimeStepDO> timeStepDOs = valueRepo.findTimeStepWithAValueEntry(epoch);
        for (TimeStepDO timeStepDO : timeStepDOs) {
            runOnTimeStepLevel(timeStepDO, epoch, n);
        }
    }


    public Optional<ValueDO> getValueDO(List<ValueDO>valueDOs, int epoch) {
        for(ValueDO valueDO : valueDOs) {
            if (valueDO.getEpoch() == epoch) return Optional.of(valueDO);
        }
        return Optional.empty();
    }


    public void runOnTimeStepLevel(TimeStepDO timeStepDO, int epoch, int n) {
        int trainingEpoch = timeStepDO.getEpisode().getTrainingEpoch();

        List<ValueDO> valueDOs = valueRepo.findValuesForTimeStepId(timeStepDO.getId());

        double sum = 0d;
        long count = 0;
        for (int i = epoch; i >= 0 && i > epoch - n && i >= trainingEpoch; i--) {
            sum += getValueDO(valueDOs, i).orElseThrow(MuZeroException::new).getValue();
            count++;
        }
        double valueMean = sum / count;
        sum = 0d;
        //  count = 0;
        for (int i = epoch; i >= 0 && i > epoch - n && i >= trainingEpoch; i--) {
            double vHat = valueMean - getValueDO(valueDOs, i).orElseThrow(MuZeroException::new).getValue();
            sum += vHat * vHat;
            // count++;
        }
        double vHatSquaredMean = sum / count;

        ValueDO valueDO = getValueDO(valueDOs, epoch).orElseThrow(MuZeroException::new);
        valueDO.setValueMean(valueMean);
        valueDO.setCount(count);
        valueDO.setValueHatSquaredMean(vHatSquaredMean);
        valueRepo.save(valueDO);

    }

}
