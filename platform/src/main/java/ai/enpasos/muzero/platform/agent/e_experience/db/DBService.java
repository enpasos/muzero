package ai.enpasos.muzero.platform.agent.e_experience.db;


import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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
    TimestepRepo timestepRepo;

    @Autowired
    MuZeroConfig config;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<EpisodeDO> saveEpisodesAndCommit(List<EpisodeDO> episodes) {
        return episodeRepo.saveAllAndFlush(episodes);
    }

    @Transactional
    public List<EpisodeDO> findTopNByOrderByIdDescAndConvertToGameDTOList(int n) {
        List<Long> ids = episodeRepo.findTopNEpisodeIds(n);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOs(ids);
        return result;
    }

    @Transactional
    public List<EpisodeDO> findRandomNByOrderByIdDescAndConvertToGameDTOList(int n) {
        List<Long> ids = episodeRepo.findRandomNEpisodeIds(n);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOs(ids);
        return result;
    }

    public int getMaxTrainingEpoch() {
        return episodeRepo.getMaxTrainingEpoch();
    }


    @Transactional
    public List<EpisodeDO> findEpisodeDOswithTimeStepDOsAndValues(List<Long> episodeIds) {


        List<TimeStepDO> timeStepDOs =  timestepRepo.findTimeStepDOswithEpisodeIds(episodeIds);
timeStepDOs.stream().forEach(t -> t.getValues().size());

        List<EpisodeDO> episodeDOs = timeStepDOs.stream().map(t -> {
            EpisodeDO episodeDO = t.getEpisode();

            return episodeDO;
        }).distinct().collect(Collectors.toList());
        episodeDOs.forEach(e -> e.setTimeSteps(new ArrayList<>()));
        timeStepDOs.stream().forEach(t ->  t.getEpisode().getTimeSteps().add(t) );
        timeStepDOs.stream().forEach(t -> t.getValues().size());

      //  List<TimeStepDO> timeStepDOS = episodeDOS.stream().map(e -> e.getTimeSteps()).flatMap(List::stream).collect(Collectors.toList());
    //    List<TimeStepDO> timeStepDOS2 = timestepRepo.joinFetchValues(timeStepDOS);

        // todo: make more performant by using join fetch

      // episodeDOS.stream().forEach(e -> e.getTimeSteps().stream().forEach(t -> t.getValues().size()));
       //
        return episodeDOs;
    }

}
