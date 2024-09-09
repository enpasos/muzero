package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortTimestep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@SuppressWarnings("ALL")
public interface TimestepRepo extends JpaRepository<TimeStepDO, Long> {

    @Transactional
    @Query(value = "select t from TimeStepDO t JOIN FETCH t.episode e where e.id in :ids ORDER BY e.id DESC, t.t ASC")
    List<TimeStepDO> findTimeStepDOswithEpisodeIds(List<Long> ids);


    @Transactional
    @Modifying
    @Query(value = "update timestep t set archived = e.archived from episode e where t.episode_id = e.id", nativeQuery = true)
    void markArchived();


    @Transactional
    @Modifying
    @Query(value = "DROP TABLE IF EXISTS  timestep CASCADE", nativeQuery = true)
    void dropTable();

    @Transactional
    @Modifying
    @Query(value = "DROP SEQUENCE IF EXISTS  timestep_seq CASCADE", nativeQuery = true)
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
    @Query(value = """
            UPDATE TimeStepDO t
            SET 
                t.rewardLoss = :rewardLoss,
                t.legalActionLossMax = :legalActionLoss,
                t.boxA = :newBox
            WHERE t.id = :id
            """)
    void updateRewardLoss(
            long id,
            float rewardLoss,
            float legalActionLoss,
            int newBox
    );

    @Transactional
    @Query(value = """
            SELECT 
                t.episode_id
            FROM timestep t
            WHERE t.a_weight_class = :groupClass
            ORDER BY RANDOM()
            LIMIT :n
            """, nativeQuery = true)
    List<Long> findNRandomEpisodeIdsWeightedA(
            int groupClass,
            int n
    );

    @Transactional
    @Query(value = """
            SELECT 
                t.episode_id
            FROM timestep t
            WHERE t.reward_loss > :rewardLossThreshold
            ORDER BY RANDOM()
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Long> findRandomNEpisodeIdsRelevantForRewardLearning(
            double rewardLossThreshold,
            int limit,
            int offset
    );

    @Transactional
    @Query(value = """
            SELECT 
                t.episode_id
            FROM timestep t
            WHERE t.legal_action_loss_max > :legalActionLossMaxThreshold
            ORDER BY RANDOM()
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Long> findRandomNEpisodeIdsRelevantForLegalActionLearning(
            double legalActionLossMaxThreshold,
            int limit,
            int offset
    );

    @Transactional
    @Query(value = """
            SELECT 
                t.episode_id
            FROM timestep t
            WHERE t.boxa = :box
            ORDER BY RANDOM()
            LIMIT :n
            GROUP BY t.episode_id
            """, nativeQuery = true)
    List<Long> findRandomNEpisodeIdsFromBox(
            int n,
            int box
    );


    @Transactional
    @Query(value = "SELECT count(*) FROM  TimeStepDO t where t.boxA = 0")
    long numBoxA0();

    @Transactional
    @Query(value = "SELECT count(*) FROM  TimeStepDO t where t.boxB = 0")
    long numBoxB0();

    @Transactional
    @Query(value = "SELECT max(t.boxA) FROM  timestep t", nativeQuery = true)
    int maxBoxA();

    @Transactional
    @Query(value = "SELECT max(t.boxB) FROM  timestep t", nativeQuery = true)
    int maxBoxB();

    @Query(value = """
            SELECT DISTINCT 
                t.u_ok
            FROM timestep t
            WHERE NOT t.u_ok_closed
            ORDER BY t.u_ok ASC
            """, nativeQuery = true)
    List<Integer> uOkList();


    @Transactional
    @Modifying
    @Query(value = """
            UPDATE TimeStepDO t
            SET 
                t.s = :s,
                t.sClosed = :sClosed,
                t.uOk = :uOk,
                t.uOkClosed = :uOkClosed,
                t.boxA = :boxA,
                t.boxB = :boxB,
                t.trainable = (t.nextUOk >= t.nextuoktarget)
            WHERE t.id = :id
            """)
    void updateAttributeSAndU(
            Long id,
            long s,
            boolean sClosed,
            long uOk,
            boolean uOkClosed,
            long boxA,
            long boxB
    );


    @Transactional
    @Modifying
    @Query(value = "UPDATE timestep SET u_ok = -2, u_ok_closed = false", nativeQuery = true)
    void resetUOk();

    @Query(value = """
            SELECT 
                t.episode_id AS episodeId,
                t.id AS id
            FROM timestep t
            WHERE t.boxa IN :boxesRelevant
            ORDER BY t.episode_id, t.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<IdProjection> getRelevantIdsA3(
            int limit,
            int offset,
            List<Integer> boxesRelevant
    );

    @Query(value = """
            SELECT 
                t.u_ok AS uOk,
                t.episode_id AS episodeId,
                t.id AS id
            FROM timestep t
            WHERE t.boxa = :box
            ORDER BY t.u_ok
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<IdProjection2> getRelevantIds5(
            int limit,
            int offset,
            int box
    );

    @Query(value = """
            SELECT 
                t.episode_id AS episodeId,
                t.id AS id
            FROM timestep t
            WHERE t.boxa != 0
            ORDER BY RANDOM()
            LIMIT :n
            """, nativeQuery = true)
    List<IdProjection> getRandomIdsNotInBox0(
            int n
    );


    @Query(value = """
            SELECT 
                t.id AS id,
                t.episode_id AS episodeId,
                t.boxa AS box,
                t.u_ok AS uOk,
                t.trainable AS trainable,
                t.t AS t
            FROM timestep t
            ORDER BY t.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<IdProjection3> getTimeStepIds3(
            int limit,
            int offset);


    @Query(value = """
            SELECT 
                t.id AS id,
                t.episode_id AS episodeId,
                t.boxa AS boxA,
                t.boxb AS boxB,
                t.u_ok AS uOk,
                t.nextuok AS nextUOk,
                t.nextuoktarget AS nextUOkTarget,
                t.t AS t,
                t.u_ok_closed AS uOkClosed
            FROM timestep t
            ORDER BY t.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> getShortTimestepList(int limit, int offset);

    @Query(value = """
            SELECT 
                new ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortTimestep(
                    ts.id,
                    ts.episode.id,
                    ts.boxA,
                    ts.boxB,
                    ts.uOk,
                    ts.nextUOk,
                    ts.nextuoktarget,
                    ts.t,
                    ts.uOkClosed
                )
            FROM TimeStepDO ts
            WHERE ts.id IN :ids
            """)
    List<ShortTimestep> getShortTimestepList(
            List<Long> ids
    );

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE TimeStepDO t
            SET 
                t.nextUOk = :nextUOk,
                t.trainable = (:nextUOk >= t.nextuoktarget OR t.uOk < 1)
            WHERE t.id = :id
            """)
    void updateNextUOk(
            long id,
            int nextUOk
    );

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE TimeStepDO t
            SET t.nextuoktarget = LEAST(
                :unrollSteps - 1,
                (SELECT e.tmax FROM EpisodeDO e WHERE e.id = t.episode.id) - t.t - 1
            )
            """)
    void updateNextUOkTarget(
            int unrollSteps
    );


}

