package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EpisodeRepo extends JpaRepository<EpisodeDO,Long> {

    @Transactional
    @Query(value = "select e from EpisodeDO e JOIN FETCH e.timeSteps ORDER BY e.id DESC")
    List<EpisodeDO> findTopNByOrderByIdDesc(Pageable pageable);


    @Query(value = "select max(e.trainingEpoch) from EpisodeDO e")
    int getMaxTrainingEpoch ();

}
