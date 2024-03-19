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

    @Query(value = "select e.id from episode e where e.archived = false order by e.id asc", nativeQuery = true)
    List<Long> findNonArchivedEpisodeIds();


    @Transactional
    @Query(value = "select distinct e.training_epoch from episode e order by e.training_epoch", nativeQuery = true)
    List<Integer> findEpochs();

    @Transactional
    @Query(value = "select e from EpisodeDO e JOIN FETCH e.timeSteps t where e.id in :ids ORDER BY e.id DESC, t.t ASC")
    List<EpisodeDO> findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(List<Long> ids);


    @Transactional
    @Query(value = "select e.id from episode e where e.max_box = :maxBox  order by e.id LIMIT :limit  OFFSET :offset", nativeQuery = true)
    List<Long> findAllEpisodeIdsWithMaxBox(int limit, int offset, int maxBox);


    @Query(value = "select max(e.trainingEpoch) from EpisodeDO e")
    int getMaxTrainingEpoch ();

    @Transactional
    @Query(value = "select e.id from episode e order by random() limit :n", nativeQuery = true)
    List<Long> findRandomNEpisodeIds(int n);



    @Transactional
    @Query(value = "select e.id from episode e order by e.rule_loss desc limit :n", nativeQuery = true)
    List<Long> findNEpisodeIdsWithHighestLoss(int n);



    @Transactional
    @Query(value = "select e.id from episode e where e.training_epoch = :epoch and e.archived = false", nativeQuery = true)
    List<Long> findAllNonArchivedEpisodeIdsForAnEpoch(int epoch);


    @Transactional
    @Modifying
    @Query(value = "update episode set archived = (max_value_variance < :quantile)", nativeQuery = true )
    void markArchived( double quantile);


    @Modifying
    @Transactional
    @Query(value = "update episode e set \n" +
            "\tmax_value_variance = e2.value_variance,  \n" +
            "\tvalue_count = e2.value_count,  \n" +
            "\tt_of_max_value_variance = e2.t\n" +
            "\tfrom (SELECT a.episode_id, a.value_variance, a.value_count, a.t FROM (\n" +
            "            SELECT episode_id, value_variance, value_count, t,\n" +
            "                   row_number() OVER (PARTITION BY episode_id ORDER BY value_variance DESC) as row_number\n" +
            "            FROM timestep\n" +
            "                    ) as a where row_number = 1) as e2 \n" +
            "\twhere e2.episode_id = e.id", nativeQuery = true )
    void aggregateMaxVarianceFromTimestep();


    @Transactional
   // @Query(value = "select min(e2.max_value_variance) from (select e.max_value_variance from episode e where e.value_count = :valueCount and e.archived = false group by e.max_value_variance order by e.max_value_variance desc  limit :n) e2", nativeQuery = true )
    @Query(value = "select min(e3.max_value_variance) from (select e2.max_value_variance from (select e.max_value_variance from episode e where e.value_count = :valueCount and e.archived = false order by e.max_value_variance desc  limit :n) e2 group by e2.max_value_variance) e3", nativeQuery = true )
    Double findTopQuantileWithHighestVariance( int n, int valueCount);


    @Transactional
    @Query(value = "select e.id, e.t_of_max_value_variance from episode e where e.archived = false order by e.max_value_variance desc limit :nGamesNeeded", nativeQuery = true)
   // @Query(value = "select e.id, e.t_of_max_value_variance from episode e where e.archived = false order by random() limit :nGamesNeeded", nativeQuery = true)
    List<Tuple> findEpisodeIdsWithHighValueVariance(int nGamesNeeded);


    @Transactional
    @Modifying
    @Query(value = "DROP TABLE IF EXISTS episode  CASCADE", nativeQuery = true )
    void dropTable();

    @Transactional
    @Modifying
    @Query(value = "DROP SEQUENCE IF EXISTS episode_seq CASCADE", nativeQuery = true )
    void dropSequence();


//    @Transactional
//    @Modifying
//    @Query(value = "update EpisodeDO e set e.ruleLoss = :loss where e.id = :id")
//    void updateRuleLoss(long id, float loss);
//
//
//    @Transactional
//    @Modifying
//    @Query(value = "update EpisodeDO e set e.ruleLoss = 0")
//    void initRuleLoss();


    @Transactional
    @Modifying
    @Query(value = "update episode e set max_box = t.box from SELECT t.episode_id, max(t.box) FROM timestep t group by t.episode_id where e.id = t.episode_id", nativeQuery = true )
    void updateMaxBox(  );
}
