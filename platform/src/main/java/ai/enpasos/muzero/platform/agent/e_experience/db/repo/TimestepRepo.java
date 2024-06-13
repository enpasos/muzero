package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


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
    @Query(value = "SELECT t.episode_id FROM timestep t WHERE t.box = :box order by random() limit :n ", nativeQuery = true)

    List<Long> findRandomNEpisodeIdsFromBox(int n, int box);





    @Transactional
    @Query(value = "SELECT count(*) FROM  timestep t where box = :box", nativeQuery = true)
    int numBox(int box);



    @Transactional
    @Query(value = "SELECT max(t.box) FROM  timestep t", nativeQuery = true)
    int maxBox( );


    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set t.s = :s, t.sClosed = :sClosed where t.id = :id" )
    void updateAttributeS(Long id, long s, boolean sClosed);

    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set t.s = :s, t.sClosed = :sClosed, t.uOk = :uOk, t.box = :box where t.id = :id" )
    void updateAttributeSAndU(Long id, long s, boolean sClosed, long uOk, long box);

    @Transactional
    @Modifying
    @Query(value = "update TimeStepDO t set t.uOk = :u where t.id = :id" )
    void updateAttributeUOk(Long id, long u);

//    @Transactional
//    @Modifying
//    @Query(value = "update TimeStepDO t set t.uOk = -2" )
//    void resetUOk();

    @Transactional
    @Modifying
    @Query(value = "UPDATE timestep SET u_ok = -2", nativeQuery = true )
    void resetUOk();
}

