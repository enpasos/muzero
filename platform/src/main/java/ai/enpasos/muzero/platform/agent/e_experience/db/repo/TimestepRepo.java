package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortTimestep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


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
    @Query(value = "update TimeStepDO t set t.rewardLoss = :rewardLoss, t.legalActionLossMax = :legalActionLoss, t.boxA = :newBox where t.id = :id")
    void updateRewardLoss(long id, float rewardLoss, float legalActionLoss, int newBox);



    @Transactional
    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.a_weight_class = :groupClass ORDER BY RANDOM() LIMIT :n", nativeQuery = true)
    List<Long> findNRandomEpisodeIdsWeightedA(int groupClass, int n );



    @Transactional
    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.reward_loss > :rewardLossThreshold order by random() limit :limit OFFSET :offset", nativeQuery = true)

    List<Long> findRandomNEpisodeIdsRelevantForRewardLearning(double rewardLossThreshold, int limit, int offset);


    @Transactional
    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.legal_action_loss_max > :legalActionLossMaxThreshold order by random() limit :limit  OFFSET :offset", nativeQuery = true)

    List<Long> findRandomNEpisodeIdsRelevantForLegalActionLearning(double legalActionLossMaxThreshold, int  limit, int offset);


    @Transactional
    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.boxa = :box order by random() limit :n group by t.episode_id ", nativeQuery = true)

    List<Long> findRandomNEpisodeIdsFromBox(int n, int box);





    @Transactional
    @Query(value = "SELECT count(*) FROM  TimeStepDO t where t.boxA = 0")
    long numBoxA0();

    @Transactional
    @Query(value = "SELECT count(*) FROM  TimeStepDO t where t.boxB = 0")
    long numBoxB0();

    @Transactional
    @Query(value = "SELECT max(t.boxA) FROM  timestep t", nativeQuery = true)
    int maxBoxA( );

    @Transactional
    @Query(value = "SELECT max(t.boxA) FROM  timestep t", nativeQuery = true)
    int maxBoxB( );



    @Transactional
    @Query(value = "SELECT min(t.u_ok) FROM  timestep t", nativeQuery = true)
    int minUOk( );


    @Query(value = "SELECT max(t.u_ok) FROM  timestep t", nativeQuery = true)
    int maxUOk( );


    @Query(value = "SELECT DISTINCT t.u_ok FROM timestep t WHERE not t.u_ok_closed ORDER BY t.u_ok ASC", nativeQuery = true)
    List<Integer> uOkList();


    @Query("SELECT t.boxA as box, COUNT(t) as count FROM TimeStepDO t GROUP BY t.boxA ORDER BY t.boxA ASC")
   List<BoxOccupation> boxOccupation();

    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set t.s = :s, t.sClosed = :sClosed, t.uOk = :uOk, t.uOkClosed = :uOkClosed,   t.boxA = :box, t.trainable = (t.nextUOk >= t.nextuoktarget)  where t.id = :id" )
    void updateAttributeSAndU(Long id, long s, boolean sClosed, long uOk, boolean uOkClosed, long box  );



    @Transactional
    @Modifying
    @Query(value = "UPDATE timestep SET u_ok = -2, u_ok_closed = false", nativeQuery = true )
    void resetUOk();



    @Query(value = "SELECT t.episode_id FROM timestep t WHERE NOT t.u_ok_closed AND t.u_ok = :uok GROUP BY t.episode_id ORDER BY t.episode_id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> getRelevantEpisodeIds2(int limit, int offset, int uok);


    @Query(value = "SELECT t.id FROM timestep t WHERE NOT t.u_ok_closed AND t.u_ok = :uok  ORDER BY t.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> getRelevantIds(int limit, int offset, int uok);

    @Query(value = "SELECT t.episode_id AS episodeId, t.id AS id FROM timestep t WHERE NOT t.u_ok_closed AND t.u_ok > :uOKMin AND t.u_ok <= :uOKMax  ORDER BY t.episode_id, t.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection> getRelevantIds2(int limit, int offset,  int uOKMin, int uOKMax);


    @Query(value = "SELECT t.episode_id AS episodeId, t.id AS id FROM timestep t WHERE t.boxa in :boxesRelevant ORDER BY t.episode_id, t.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection> getRelevantIdsA3(int limit, int offset, List<Integer> boxesRelevant);




    @Query(value = "SELECT t.episode_id AS episodeId, t.id AS id FROM timestep t WHERE t.boxa = 0 ORDER BY t.episode_id, t.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection> getRelevantIds4(int limit, int offset);

    @Query(value = "SELECT t.u_ok AS uOk, t.episode_id AS episodeId, t.id AS id FROM timestep t WHERE t.boxa = :box ORDER BY t.u_ok LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection2> getRelevantIds5(int limit, int offset, int box);


    @Query(value = "SELECT t.episode_id AS episodeId, t.id AS id FROM timestep t WHERE t.boxa != 0 ORDER BY RANDOM() LIMIT :n", nativeQuery = true)
    List<IdProjection> getRandomIdsNotInBox0(int n);

    @Query(value = "SELECT t.episode_id AS episodeId, t.id AS id FROM timestep t WHERE t.boxa = 0 ORDER BY RANDOM() LIMIT :n", nativeQuery = true)
    List<IdProjection> getRandomIdsInBox0(int n);

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




    @Query(value = "SELECT t.episode_id AS episodeId, t.id AS id  FROM time_steps t JOIN episodes e ON t.episode_id = e.id WHERE t.unroll_steps = :unrollSteps ORDER BY RANDOM() LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection> findTimeStepIdsByUnrollSteps(int unrollSteps, int limit, int offset);





    @Query(value = "SELECT t.id AS id, t.episode_id AS episodeId FROM timestep t JOIN episode e ON t.episode_id = e.id WHERE t.boxa IN :boxesRelevant and t.nextuok >= t.nextuoktarget ORDER BY e.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection> getTimeStepIdsByBoxesRelevant(
            List<Integer> boxesRelevant,
            int limit,
            int offset);

    @Query(value = "SELECT t.id AS id, t.episode_id AS episodeId FROM timestep t JOIN episode e ON t.episode_id = e.id WHERE t.boxa IN :boxesRelevant   ORDER BY e.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection> getTimeStepIdsByBoxesRelevant0(

            List<Integer> boxesRelevant,
            int limit,
            int offset);

    @Query(value = "SELECT t.id AS id, t.episode_id AS episodeId FROM timestep t JOIN episode e ON t.episode_id = e.id WHERE t.boxa = :box  ORDER BY e.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection> getTimeStepIdsInBox(

            int box,
            int limit,
            int offset);

    @Query(value = "SELECT t.id AS id, t.episode_id AS episodeId, t.boxa AS box, t.u_ok as uOk, t.trainable AS trainable, t.t AS t FROM timestep t  ORDER BY t.id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<IdProjection3> getTimeStepIds3(
            int limit,
            int offset);



//    @Query(value = """
//        SELECT
//            t.id AS id,
//            t.episode_id AS episodeId,
//            t.box AS box,
//            t.u_ok as uOk,
//            t.nextuok as nextUOk,
//            t.nextuoktarget as nextUOkTarget,
//            t.t AS t
//        FROM timestep t
//        ORDER BY t.id
//        LIMIT :limit OFFSET :offset
//        """,
        @Query(value = "SELECT t.id AS id,  t.episode_id AS episodeId,  t.boxa AS boxA, t.boxb AS boxB,  t.u_ok as uOk,  t.nextuok as nextUOk,  t.nextuoktarget as nextUOkTarget,  t.t AS t  FROM timestep t ORDER BY t.id LIMIT :limit OFFSET :offset", nativeQuery = true)
        List<Object[]> getShortTimestepList(int limit, int offset);



    @Query(value = "SELECT new ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortTimestep( ts.id  , ts.episode.id  ,  ts.boxA,  ts.boxB  ,  ts.uOk  ,  ts.nextUOk  ,  ts.nextuoktarget  ,  ts.t  )  FROM TimeStepDO ts where ts.id in :ids ")
   // @SqlResultSetMapping(name = "ShortTimestepMapping")
    List<ShortTimestep> getShortTimestepList(List<Long> ids) ;



//    @Query(value = "SELECT min(ts.unrollSteps) FROM TimeStepDO ts")
//    default int minUnrollSteps() {
//        return 1;
//    }
//
//
//    @Query("SELECT ts.unrollSteps as unrollSteps, COUNT(ts.id) as count FROM TimeStepDO ts WHERE Not ts.uOkClosed GROUP BY ts.unrollSteps ORDER BY ts.unrollSteps ASC")
//    List<UnrollStepsCount> countTimeStepsByUnrollSteps();


    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set t.nextUOk = :nextUOk, t.trainable = (:nextUOk >= t.nextuoktarget or t.uOk < 1) where t.id = :id" )
    void updateNextUOk(long id, int nextUOk);


    @Transactional
    @Modifying
    @Query("UPDATE TimeStepDO t SET t.nextuoktarget = LEAST(:unrollSteps - 1, (SELECT e.tmax FROM EpisodeDO e WHERE e.id = t.episode.id) - t.t - 1)")
    void updateNextUOkTarget(int unrollSteps);


//    @Transactional
//    @Modifying
//    @Query("UPDATE TimeStepDO t SET t.trainable = (t.nextUOk >= t.nextuoktarget or t.uOk < 1)  ")
//    void updateTrainable( );


    @Transactional
    @Modifying
    @Query("UPDATE TimeStepDO t SET t.boxA = 0 WHERE t.uOk < :unrollSteps AND NOT t.uOkClosed")
    void updateBox0(int unrollSteps);
}

