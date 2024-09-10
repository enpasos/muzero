package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EpisodeRepo extends JpaRepository<EpisodeDO,Long> {

    @Transactional
    @Query(value = "select e.id from episode e order by e.id desc limit :n", nativeQuery = true)
    List<Long> findTopNEpisodeIds(int n);



    @Transactional
    @Query(value = "select e from EpisodeDO e JOIN FETCH e.timeSteps t where e.id in :ids ORDER BY e.id DESC, t.t ASC")
    List<EpisodeDO> findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(List<Long> ids);




    @Transactional
    @Query(value = "select e.id from episode e where e.minuok <= :minuok  order by e.id LIMIT :limit  OFFSET :offset", nativeQuery = true)
    List<Long> findAllEpisodeIdsWithBoxSmallerOrEqualsMinUOk(int limit, int offset, int minuok);


    @Transactional
    @Query(value = "select e.id from episode e   order by e.id LIMIT :limit  OFFSET :offset", nativeQuery = true)
    List<Long> findAllEpisodeIds(int limit, int offset );
    @Query(value = "select max(e.trainingEpoch) from EpisodeDO e")
    int getMaxTrainingEpoch ();

    @Transactional
    @Query(value = "select e.id from episode e order by random() limit :n", nativeQuery = true)
    List<Long> findRandomNEpisodeIds(int n);









    @Transactional
    @Modifying
    @Query(value = "DROP TABLE IF EXISTS episode  CASCADE", nativeQuery = true )
    void dropTable();

    @Transactional
    @Modifying
    @Query(value = "DROP SEQUENCE IF EXISTS episode_seq CASCADE", nativeQuery = true )
    void dropSequence();






    @Transactional
    @Modifying
    @Query(value = "UPDATE episode e \n" +
            "SET minuok = t.minuok\n" +
            "FROM (\n" +
            "    SELECT episode_id, MIN(u_ok) as minuok\n" +
            "    FROM timestep\n" +
            "    GROUP BY episode_id\n" +
            ") t\n" +
            "WHERE e.id = t.episode_id", nativeQuery = true )
    void updateMinUOK(  );










}
