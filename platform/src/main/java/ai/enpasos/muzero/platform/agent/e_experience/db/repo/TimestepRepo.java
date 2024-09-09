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
    @Query(value = "DROP TABLE IF EXISTS  timestep CASCADE", nativeQuery = true)
    void dropTable();

    @Transactional
    @Modifying
    @Query(value = "DROP SEQUENCE IF EXISTS  timestep_seq CASCADE", nativeQuery = true)
    void dropSequence();




    @Transactional
    @Query(value = "SELECT count(*) FROM  TimeStepDO t where not t.uOkClosed")
    long numNotClosed();




    @Query(value = "SELECT MAX(array_length(t.boxes, 1)) FROM timestep t", nativeQuery = true)
    int maxBox();

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
                t.boxes = :boxes,
                t.trainable = (t.nextUOk >= t.nextuoktarget)
            WHERE t.id = :id
            """)
    void updateAttributeSAndU(
            Long id,
            long s,
            boolean sClosed,
            long uOk,
            boolean uOkClosed,
            Integer[] boxes
    );


    @Transactional
    @Modifying
    @Query(value = "UPDATE timestep SET u_ok = -2, u_ok_closed = false", nativeQuery = true)
    void resetUOk();




    @Query(value = """
            SELECT 
                t.id AS id,
                t.episode_id AS episodeId,
                t.boxes AS boxes, 
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
                    ts.boxes,
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

