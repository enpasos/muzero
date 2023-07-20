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
}
