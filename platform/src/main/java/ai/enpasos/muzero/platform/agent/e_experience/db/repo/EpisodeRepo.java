package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EpisodeRepo extends JpaRepository<EpisodeDO,Long> {

    @Transactional
    @Query(value = "select * from episode e  ORDER BY e.id DESC LIMIT :n", nativeQuery = true)
    List<EpisodeDO> findTopNByOrderByIdDesc(@Param("n") int n);


//    @Transactional
//    List<EpisodeDO> findTop100ByOrderByIdDesc();
}
