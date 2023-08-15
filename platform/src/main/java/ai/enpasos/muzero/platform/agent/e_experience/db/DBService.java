package ai.enpasos.muzero.platform.agent.e_experience.db;


import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DBService {

    @Autowired
    EpisodeRepo episodeRepo;


    @Autowired
    ValueRepo valueRepo;


    @Autowired
    TimestepRepo timestepRepo;

    @Autowired
    MuZeroConfig config;


    public void clearDB() {
        episodeRepo.dropTable();
        episodeRepo.dropSequence();
        timestepRepo.dropTable();
        timestepRepo.dropSequence();
        valueRepo.dropTable();
        valueRepo.dropSequence();
    }

    // @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    @Transactional
    public List<EpisodeDO> findNGamesToMemorize(int n) {
        List<Long> ids = episodeRepo.findMRandomFromNEpisodesWithWorstMemorizedReward( 5*n, n);
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
    public void setValueHatSquaredMeanForTimeStep (TimeStepDO timeStepDO,  int n) {
        //int trainingEpoch = timeStepDO.getEpisode().getTrainingEpoch();

        List<ValueDO> valueDOs = valueRepo.findNonArchivedValuesForTimeStepId(timeStepDO.getId());

        double sum = 0d;
        long count = 0;
        for (ValueDO valueDO : valueDOs) {
            sum += valueDO.getValue();
            count++;
        }

        double valueMean = sum / count;
        sum = 0d;
        for (ValueDO valueDO : valueDOs) {
            double vHat = valueMean - valueDO.getValue();
            sum += vHat * vHat;
        }
        double vHatSquaredMean = sum / count;


        ValueDO valueDO = ValueRepo.extractValueDOMaxEpoch(valueDOs).orElseThrow(MuZeroException::new);

//        valueDO.setValueMean(valueMean);
//        valueDO.setCount(count);
//        valueDO.setValueHatSquaredMean(vHatSquaredMean);


    }

    //@Transactional
    public void markArchived(int epoch) {
        int n = 10000; // todo
        int n2 = 10;
       // int nCandidate = episodeRepo.countNotArchivedWithValueCount(n2);
       // if (nCandidate < n) return;
        Double quantile = episodeRepo.findTopQuantileWithHighestVariance(n, n2);
        log.info("quantile: {}", quantile);
        if (quantile == null) return;
        log.info("episodeRepo.markArchived(quantile) ...");
        episodeRepo.markArchived(quantile);
        log.info("timestepRepo.markArchived() ...");
        timestepRepo.markArchived();
        log.info("valueRepo.markArchived() ...");
        valueRepo.markArchived();
        log.info("...markArchived.");
    }
}
