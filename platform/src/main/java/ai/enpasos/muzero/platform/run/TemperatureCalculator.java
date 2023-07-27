package ai.enpasos.muzero.platform.run;


import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.NetworkIOService;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueStatsDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueStatsRepo;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

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
    ValueStatsRepo valueStatsRepo;
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

    public void aggregatePerEpisode(int epoch, int n) {
        valueStatsRepo.deleteAllInBatch();
        DecimalFormat df = new DecimalFormat("#,###,###,##0.0000000");
        List<Long> episodeIds = episodeRepo.findNonArchivedEpisodeIds();
        int count = 1;

        log.debug("aggregatePerEpisode ... episodeIds.size(): {} ",  episodeIds.size());
        for (Long episodeId : episodeIds) {
            List<ValueDO> valueDOs = valueRepo.findValuesForEpochAndEpisodeIdWithCountEqualsNAndNotArchived(epoch, episodeId, n);

            List<ValueStatsDO> statsDOs = new ArrayList<>();
            List<ValueDO> valueDOsForEpoch = valueDOs.stream().filter(v -> v.getEpoch() == epoch).collect(Collectors.toList());
            double maxValue = 0d;
            int maxT = -1;
            for (int i = 0; i < valueDOsForEpoch.size(); i++) {
                ValueDO valueDO = valueDOsForEpoch.get(i);
                double v = valueDO.getValueHatSquaredMean();
                if (v > maxValue) {
                    maxValue = v;
                    maxT = valueDO.getTimestep().getT();
                }
            }

            ValueStatsDO valueStatsDO = ValueStatsDO.builder()
                    .maxValueHatSquaredMean(maxValue)
                    .tOfMaxValueHatSquaredMean(maxT)
                    .epoch(epoch)
                    .build();

            statsDOs.add(valueStatsDO);

            dbService.saveValueStats(statsDOs, episodeId);
        }

    }

    public void aggregatePerEpoch() {
        List<Integer> epochs = episodeRepo.findEpochs();
        DecimalFormat df = new DecimalFormat("#,###,###,##0.0000000");

        log.info("temperature aggregation for epochs {}", epochs);
        for (Integer epoch : epochs) {

            double temperature = aggregateOnEpoch(epoch);

            System.out.println(epoch + ";" + df.format(temperature));
        }
    }

    private double aggregateOnEpoch(Integer epoch) {
        List<ValueDO> valueDOs = valueRepo.findValuesForEpoch(epoch);
        Pair<Double, Long> pair = aggregateValues(valueDOs);
        return pair.getFirst() / pair.getSecond();
    }

    private static Pair<Double, Long> aggregateValues(List<ValueDO> valueDOs) {
        double sum = 0d;
        long count = 0;
        for(int i = 0; i < valueDOs.size(); i++) {
            ValueDO valueDO = valueDOs.get(i);
            long c = valueDO.getCount();
            count += c;
            sum += c * valueDO.getValueHatSquaredMean();
        }
        return Pair.of(sum, count);
      //  return sum / count;
    }


    public void setValueHatSquaredMeanForEpochWithSummationOverLastNEpochs(int startEpoch) {
        int n = 10;
        List<Integer> epochs = episodeRepo.findEpochs();
        for (Integer epoch : epochs) {
            if (epoch < startEpoch) continue;
            log.info("temperature calculating for epoch {}", epoch);
            setValueHatSquaredMeanForEpochWithSummationOverLastNEpochs(epoch, n);
        }
    }


    public void setValueHatSquaredMeanForEpochWithSummationOverLastNEpochs(int epoch, int n) {
        valueRepo.archiveValueOlderThanGivenEpoch(epoch - n + 1);
        valueRepo.deleteArchived();
        int maxEpoch = valueRepo.getMaxEpoch();
        log.debug("setValueHatSquaredMeanForEpochWithSummationOverLastNEpochs ... epoch= {}; maxEpoch= {}", epoch,  maxEpoch );
        List<TimeStepDO> timeStepDOs = valueRepo.findNonExploringNonArchivedTimeStepWithAValueEntry(maxEpoch);
        int todo = timeStepDOs.size();
        log.debug("runOnTimeStepLevel ... todo: {}",   todo );
        int count = 0;
        for (TimeStepDO timeStepDO : timeStepDOs) {
            dbService.setValueHatSquaredMeanForTimeStep (timeStepDO,  n);
        }
    }


    public void markArchived() {
        dbService.markArchived();
    }
}
