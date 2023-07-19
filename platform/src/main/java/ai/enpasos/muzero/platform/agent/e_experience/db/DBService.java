package ai.enpasos.muzero.platform.agent.e_experience.db;


import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueStatsDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueStatsRepo;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DBService {

    @Autowired
    EpisodeRepo episodeRepo;


    @Autowired
    ValueRepo valueRepo;


    @Autowired
    ValueStatsRepo valueStatsRepo;


    @Autowired
    TimestepRepo timestepRepo;

    @Autowired
    MuZeroConfig config;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<EpisodeDO> saveEpisodesAndCommit(List<EpisodeDO> episodes) {

        episodes.stream().filter(EpisodeDO::isHybrid).forEach(episodeDO -> {
           long t = episodeDO.getTStartNormal();
            for (TimeStepDO timeStep : episodeDO.getTimeSteps()) {
                timeStep.setExploring(timeStep.getT() < t);
            }
        });

        return episodeRepo.saveAllAndFlush(episodes);
    }

    @Transactional
    public List<EpisodeDO> findTopNByOrderByIdDescAndConvertToGameDTOList(int n) {
        List<Long> ids = episodeRepo.findTopNEpisodeIds(n);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    @Transactional
    public List<EpisodeDO> findRandomNByOrderByIdDescAndConvertToGameDTOList(int n) {
        List<Long> ids = episodeRepo.findRandomNEpisodeIds(n);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    public int getMaxTrainingEpoch() {
        return episodeRepo.getMaxTrainingEpoch();
    }


    @Transactional
    public List<EpisodeDO> findEpisodeDOswithTimeStepDOsAndValues(List<Long> episodeIds) {

        List<TimeStepDO> timeStepDOs = timestepRepo.findTimeStepDOswithEpisodeIds(episodeIds);
        timeStepDOs.stream().forEach(t -> t.getValues().size());

        List<EpisodeDO> episodeDOs = timeStepDOs.stream().map(t -> t.getEpisode()).distinct().collect(Collectors.toList());
        episodeDOs.forEach(e -> e.setTimeSteps(new ArrayList<>()));
        timeStepDOs.stream().forEach(t -> t.getEpisode().getTimeSteps().add(t));
        timeStepDOs.stream().forEach(t -> t.getValues().size());

        return episodeDOs;
    }

    @Transactional
    public void saveValueStats(List<ValueStatsDO> statsDOs, Long episodeId) {
        valueStatsRepo.deleteByEpisodeId(episodeId);
        valueStatsRepo.flush();
        EpisodeDO episodeDO = episodeRepo.getReferenceById(episodeId);

        episodeDO.setValueStatsDOs(statsDOs);
         statsDOs.stream().forEach(s -> s.setEpisode(episodeDO))    ;
        episodeRepo.save(episodeDO);
    }




    @Transactional
    public void runOnTimeStepLevel(TimeStepDO timeStepDO, int epoch, int n) {
        int trainingEpoch = timeStepDO.getEpisode().getTrainingEpoch();

        List<ValueDO> valueDOs = valueRepo.findValuesForTimeStepId(timeStepDO.getId());

        double sum = 0d;
        long count = 0;
        for (int i = epoch; i >= 0 && i > epoch - n && i >= trainingEpoch; i--) {
            sum += ValueRepo.extractValueDO(valueDOs, i).orElseThrow(MuZeroException::new).getValue();
            count++;
        }
        double valueMean = sum / count;
        sum = 0d;
        //  count = 0;
        for (int i = epoch; i >= 0 && i > epoch - n && i >= trainingEpoch; i--) {
            double vHat = valueMean - ValueRepo.extractValueDO(valueDOs, i).orElseThrow(MuZeroException::new).getValue();
            sum += vHat * vHat;
            // count++;
        }
        double vHatSquaredMean = sum / count;


        ValueDO valueDO = ValueRepo.extractValueDO(valueDOs, epoch).orElseThrow(MuZeroException::new);
        valueDO.setValueMean(valueMean);
        valueDO.setCount(count);
        valueDO.setValueHatSquaredMean(vHatSquaredMean);

    }
}
