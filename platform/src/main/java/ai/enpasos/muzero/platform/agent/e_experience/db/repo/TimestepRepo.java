package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface TimestepRepo extends JpaRepository<TimeStepDO,Long> {

    @Transactional
      @Query(value = "select distinct t.episode_id from timestep t left join (select * from value v1 where v1.epoch = :epoch) v on t.id = v.timestep_id where t.episode_id in :episodeIds and t.action is not null  and v is null ", nativeQuery = true)
    List<Long> findEpisodeIdsForAnEpoch(int epoch, List<Long>  episodeIds);

    @Transactional
    @Query(value = "select t from TimeStepDO t JOIN FETCH t.episode e where e.id in :ids ORDER BY e.id DESC, t.t ASC")
    List<TimeStepDO> findTimeStepDOswithEpisodeIds(List<Long> ids);


    @Transactional
    @Query(value = "SELECT MAX(a_weight_cumulative) FROM timestep", nativeQuery = true)
    float getWeightACumulatedMax();


    @Modifying
    @Transactional
    @Query(value = "update timestep ts set value_count = v.valuecount,  value_mean = v.valuesum/v.valuecount from (select timestep_id, sum(w.value) as valuesum, count(w.value) as valuecount from value w  group by timestep_id) as v where v.timestep_id = ts.id", nativeQuery = true )
    void aggregateMeanFromValue();

    @Modifying
    @Transactional
    @Query(value = "update timestep ts set value_variance =  v2.valueprevariance / v2.value_count\n" +
            " from (\n" +
            "         select timestep_id,value_count, sum((w.value - w.value_mean)*(w.value - w.value_mean)) as valueprevariance\n" +
            "         from (select v.timestep_id, v.value, t.value_mean, t.value_count from value v inner join  timestep t on v.timestep_id = t.id) w\n" +
            "         group by timestep_id, value_count) as v2\n" +
            " where v2.timestep_id = ts.id", nativeQuery = true )
    void aggregateVarianceFromValue();

    @Transactional
    @Modifying
    @Query(value = "update timestep t set archived = e.archived from episode e where t.episode_id = e.id", nativeQuery = true )
   // @Query(value = "update timestep t set archived = true from episode e where t.archived = false and e.archived = true and t.episode_id = e.id", nativeQuery = true )
    void markArchived(  );


    @Transactional
    @Modifying
    @Query(value = "DROP TABLE IF EXISTS  timestep CASCADE", nativeQuery = true )
    void dropTable();

    @Transactional
    @Modifying
    @Query(value = "DROP SEQUENCE IF EXISTS  timestep_seq CASCADE", nativeQuery = true )
    void dropSequence();




    @Transactional
    @Query(value = "SELECT t.episode_id FROM timestep t where t.reward_loss > :minLoss GROUP BY t.episode_id ORDER BY MAX(t.reward_loss) DESC LIMIT :n", nativeQuery = true)
    List<Long> findNEpisodeIdsWithHighestRewardLoss(int n, double minLoss);



    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set t.legalActionLossMax = 0, t.rewardLoss = 0")
    void deleteRulesLearningResults();


    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set t.rewardLoss = :rewardLoss, t.legalActionLossMax = :legalActionLoss, t.box = :newBox where t.id = :id")
    void updateRewardLoss(long id, float rewardLoss, float legalActionLoss, int newBox);



    @Transactional
    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.a_weight_class = :groupClass ORDER BY RANDOM() LIMIT :n", nativeQuery = true)
    List<Long> findNRandomEpisodeIdsWeightedA(int groupClass, int n );


    @Transactional
    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.box < :box order by t.id limit :limit OFFSET :offset", nativeQuery = true)

    List<Long> findNEpisodeIdsRelevantForRuleLearning(int box, int limit, int offset);




    @Transactional
    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.reward_loss > :rewardLossThreshold order by random() limit :limit OFFSET :offset", nativeQuery = true)

    List<Long> findRandomNEpisodeIdsRelevantForRewardLearning(double rewardLossThreshold, int limit, int offset);


    @Transactional
    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.legal_action_loss_max > :legalActionLossMaxThreshold order by random() limit :limit  OFFSET :offset", nativeQuery = true)

    List<Long> findRandomNEpisodeIdsRelevantForLegalActionLearning(double legalActionLossMaxThreshold, int  limit, int offset);


    @Transactional
    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.box = :box order by random() limit :n group by t.episode_id ", nativeQuery = true)

    List<Long> findRandomNEpisodeIdsFromBox(int n, int box);





    @Transactional
    @Query(value = "SELECT count(*) FROM  TimeStepDO t where t.box = :box")
    long numBox(int box);



    @Transactional
    @Query(value = "SELECT max(t.box) FROM  timestep t", nativeQuery = true)
    int maxBox( );

    @Transactional
    @Query(value = "SELECT min(t.u_ok) FROM  timestep t", nativeQuery = true)
    int minUOk( );


    @Query(value = "SELECT max(t.u_ok) FROM  timestep t", nativeQuery = true)
    int maxUOk( );


    @Query(value = "SELECT DISTINCT t.u_ok FROM timestep t WHERE not t.u_ok_closed ORDER BY t.u_ok ASC", nativeQuery = true)
    List<Integer> uOkList();






    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set t.s = :s, t.sClosed = :sClosed where t.id = :id" )
    void updateAttributeS(Long id, long s, boolean sClosed);

    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set t.s = :s, t.sClosed = :sClosed, t.uOk = :uOk, t.uOkClosed = :uOkClosed, t.box = :box where t.id = :id" )
    void updateAttributeSAndU(Long id, long s, boolean sClosed, long uOk, boolean uOkClosed, long box);


    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set  t.box = :box where t.id = :id" )
    void updateAttributeBox(Long id,  long box);

    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set t.uOk = :u where t.id = :id" )
    void updateAttributeUOk(Long id, long u);



    @Transactional
    @Modifying
    @Query(value = "UPDATE timestep SET u_ok = -2, u_ok_closed = false", nativeQuery = true )
    void resetUOk();


//    @Transactional
//    @Modifying
//    @Query(value = "UPDATE timestep SET box = 0", nativeQuery = true )
//    void resetBox();
//
//    @Transactional
//    @Modifying
//    @Query(value = "UPDATE timestep SET s = 0, s_closed = false", nativeQuery = true )
//    void resetS();
//
//    @Transactional
//    @Modifying
//    @Query(value = "UPDATE timestep SET box = 0, s = 0, s_closed = false, u_ok = -2, u_ok_closed = false", nativeQuery = true )
//    void resetBoxAndSAndUOk();


//    @Transactional
//    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.box in :boxesRelevant order by t.id limit :limit OFFSET :offset", nativeQuery = true)
//    List<Long> getRelevantEpisodeIds(List<Integer> boxesRelevant, int limit, int offset);



    @Query(value = "SELECT t.episode_id FROM timestep t WHERE NOT t.u_ok_closed AND t.u_ok = :uok GROUP BY t.episode_id ORDER BY t.episode_id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> getRelevantEpisodeIds2(int limit, int offset, int uok);


    @Query(value = "SELECT t.id FROM timestep t WHERE NOT t.u_ok_closed AND t.u_ok = :uok  ORDER BY t.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> getRelevantIds(int limit, int offset, int uok);

    @Query(value = "SELECT t.episode_id AS episodeId, t.id AS id FROM timestep t WHERE NOT t.u_ok_closed AND t.u_ok > :uOKMin AND t.u_ok <= :uOKMax  ORDER BY t.episode_id, t.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection> getRelevantIds2(int limit, int offset,  int uOKMin, int uOKMax);


    @Query(value = "SELECT t.episode_id AS episodeId, t.id AS id FROM timestep t WHERE t.box in :boxesRelevant ORDER BY t.episode_id, t.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection> getRelevantIds3(int limit, int offset, List<Integer> boxesRelevant);

//    @Query(value = "SELECT t.episode_id AS episodeId, t.id AS id FROM timestep t WHERE t.box = 0 ORDER BY t.episode_id, t.id LIMIT :limit OFFSET :offset", nativeQuery = true)
//    List<IdProjection> getRelevantIds4(int limit, int offset);


    @Transactional
    @Query(value = "SELECT min(t.u_ok) FROM  timestep t WHERE not t.u_ok_closed", nativeQuery = true)
    int minUokNotClosed( );

    @Transactional
    @Query(value = "SELECT max(t.u_ok) FROM  timestep t WHERE not t.u_ok_closed", nativeQuery = true)
    int maxUokNotClosed( );

//    @Query(value = "SELECT sum(count) AS total_count\n" +
//            "FROM (\n" +
//            "    SELECT count(t.id) AS count\n" +
//            "    FROM timestep t \n" +
//            "    WHERE not t.u_ok_closed and t.u_ok + 1 <= :unrollSteps \n" +
//            "    GROUP BY t.u_ok\n" +
//            ") AS subquery;", nativeQuery = true)
//    Optional<Integer> toBeTrained(int unrollSteps);
}

