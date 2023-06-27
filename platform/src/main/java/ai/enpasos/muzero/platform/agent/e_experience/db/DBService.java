package ai.enpasos.muzero.platform.agent.e_experience.db;

//import ai.enpasos.muzero.platform.agent.e_experience.GameDTO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class DBService {

    @Autowired
    EpisodeRepo episodeRepo;

    @Autowired
    MuZeroConfig config;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<EpisodeDO> saveEpisodesAndCommit(List<EpisodeDO> episodes) {
        return episodeRepo.saveAllAndFlush(episodes);
    }



    @Transactional
    public List<EpisodeDO> findTopNByOrderByIdDescAndConvertToGameDTOList(int n) {
        return episodeRepo.findTopNByOrderByIdDesc(PageRequest.of(0, config.getWindowSize()));
    }


 public int getMaxTrainingEpoch() {
        return episodeRepo.getMaxTrainingEpoch();
 }

}
