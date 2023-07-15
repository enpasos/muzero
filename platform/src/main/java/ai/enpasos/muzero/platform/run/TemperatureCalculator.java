package ai.enpasos.muzero.platform.run;


import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.NetworkIOService;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueStatsDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueRepo;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.convertEpisodeDOsToGames;

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


    public void run() {
        int n = 10;
        List<Integer> epochs = episodeRepo.findEpochs();
        for (Integer epoch : epochs) {
            log.info("temperature calculating for epoch {}", epoch);
            run(epoch, n);
        }
    }

  //  @Transactional
    public void run(int epoch, int n) {
        List<TimeStepDO> timeStepDOs = valueRepo.findTimeStepWithAValueEntry(epoch);
        for (TimeStepDO timeStepDO : timeStepDOs) {
            run(timeStepDO, epoch, n);
        }
    }


    public Optional<ValueDO> getValueDO(List<ValueDO>valueDOs, int epoch) {
        for(ValueDO valueDO : valueDOs) {
            if (valueDO.getEpoch() == epoch) return Optional.of(valueDO);
        }
        return Optional.empty();
    }

    //@Transactional
    public void run(TimeStepDO timeStepDO, int epoch, int n) {


        List<ValueDO> valueDOs = valueRepo.findValuesForTimeStepId(timeStepDO.getId());

        double sum = 0d;
        long count = 0;
        for (int i = epoch; i >= 0 && i > epoch - n; i--) {
            sum += getValueDO(valueDOs, i).orElseThrow(MuZeroException::new).getValue();
            count++;
        }
        double valueMean = sum / count;
        sum = 0d;
        //  count = 0;
        for (int i = epoch; i >= 0 && i > epoch - n; i--) {
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
