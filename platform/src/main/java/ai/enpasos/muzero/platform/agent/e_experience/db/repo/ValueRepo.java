package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ValueRepo extends JpaRepository<ValueDO,Long> {




    // findEpisodeIdsForTimeStepsWithAMissingValueEntry


    @Transactional
    @Query(value = "select v.timestep.episode.id from ValueDO v  where v.epoch = :epoch")
    List<Long> findEpisodeIdsWithAValueEntry(int epoch);

    @Transactional
    @Modifying
    @Query(value = "insert into value (id, epoch, value, timestep_id) values (nextval('value_seq'), :epoch, :value, :timestep_id);", nativeQuery = true )
    void myInsert(int epoch, double value, long timestep_id);




}