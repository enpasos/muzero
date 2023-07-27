package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueStatsDO;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ValueStatsRepo extends JpaRepository<ValueStatsDO,Long> {


    void deleteByEpisodeId(Long episodeId);

    @Query(value = "select max(vs.epoch) from ValueStatsDO vs")
    Integer getMaxEpoch();
    @Transactional
    @Query(value = "select v.episode_id, v.t_of_max_value_hat_squared_mean from valuestats v  where v.epoch = :epoch order by v.max_value_hat_squared_mean desc limit :n ", nativeQuery = true )
    List<Tuple> findTopNEpisodeIdsWithHighestTemperatureOnTimeStep(int epoch, int n);

    @Transactional
    @Query(value = "select  min(r.value)  from  (select   v.max_value_hat_squared_mean as value from valuestats v where v.epoch = :epoch order by v.max_value_hat_squared_mean desc limit :n)   as r", nativeQuery = true )
    Double findTopQuantileWithHighestTemperatureOnTimeStep(int epoch, int n);


    @Transactional
    @Modifying
    @Query(value = "update valuestats set archived = (max_value_hat_squared_mean < :quantile) where epoch = :epoch", nativeQuery = true )
            void archiveValueStatsWithLowTemperature(int epoch, double quantile);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM valuestats v WHERE v.archived = true", nativeQuery = true )
    void deleteArchived();

    @Query(value ="select distinct v.episode_id  from valuestats v  where epoch = :epoch and v.archived = true", nativeQuery = true )
    List<Long> selectArchivedEpisodes(int epoch);

    @Query(value ="select distinct v.episode_id  from valuestats v  where epoch = :epoch and v.archived = false", nativeQuery = true )
    List<Long> selectNotArchivedEpisodes(int epoch);


    @Transactional
    @Modifying
    @Query(value = "insert into valuestats (id, epoch, max_value_hat_squared_mean, t_of_max_value_hat_squared_mean, episode_id, archived) values (nextval('valuestats_seq'), :epoch, :value, :t, :episode_id, false);", nativeQuery = true )
    void myInsert(int epoch, double value, int t, long episode_id);
}
