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
    @Query(value = "select e.id from episode e order by e.id desc limit :n", nativeQuery = true)
    List<Long> findTopNEpisodeIds(int n);

    @Query(value = "select e.id from episode e order by e.id asc", nativeQuery = true)
    List<Long> findEpisodeIds();


    @Transactional
    @Query(value = "select distinct e.training_epoch from episode e order by e.training_epoch", nativeQuery = true)
    List<Integer> findEpochs();

    @Transactional
    @Query(value = "select e from EpisodeDO e JOIN FETCH e.timeSteps t where e.id in :ids ORDER BY e.id DESC, t.t ASC")
    List<EpisodeDO> findEpisodeDOswithTimeStepDOs(List<Long> ids);




    @Query(value = "select max(e.trainingEpoch) from EpisodeDO e")
    int getMaxTrainingEpoch ();

    @Transactional
    @Query(value = "select e.id from episode e order by random() limit :n", nativeQuery = true)
    List<Long> findRandomNEpisodeIds(int n);


    @Transactional
    @Query(value = "select e.id from episode e", nativeQuery = true)
    List<Long> findAllIds();


    @Transactional
    @Query(value = "select e.id from episode e where e.training_epoch = :epoch", nativeQuery = true)
    List<Long> findAllIdsForAnEpoch(int epoch);
}
