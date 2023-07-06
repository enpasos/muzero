package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TimestepRepo extends JpaRepository<TimeStepDO,Long> {


    @Transactional
    @Query(value = "select distinct t.episode_id from timestep t left join (select * from value v1 where v1.epoch = :epoch) v on t.id = v.timestep_id where t.action is not null and v is null ", nativeQuery = true)
    List<Long> findEpisodeIdsWithoutValueForAnEpoch(int epoch);



//    @Transactional
//    @Query(value = "select t from TimeStepDO t JOIN FETCH t.values v where t in :timesteps")
//    List<TimeStepDO> joinFetchValues(List<TimeStepDO> timesteps);
}
