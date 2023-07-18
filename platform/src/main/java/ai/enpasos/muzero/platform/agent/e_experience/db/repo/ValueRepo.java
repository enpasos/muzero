package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ValueRepo extends JpaRepository<ValueDO,Long> {

    @Transactional
    @Query(value = "select v from ValueDO v  where v.epoch = :epoch and v.timestep.episode.trainingEpoch = :trainingEpoch")
    List<ValueDO> findValuesForEpochAndTrainingEpoch(int epoch, int trainingEpoch);


    @Transactional
    @Query(value = "select v from ValueDO v  where v.epoch = :epoch and v.timestep.episode.id = :episodeId")
    List<ValueDO> findValuesForEpochAndEpisodeId(int epoch, long episodeId);

    @Transactional
    @Query(value = "select v from ValueDO v  where  v.timestep.episode.id = :episodeId")
    List<ValueDO> findValuesForEpisodeId( long episodeId);


    @Transactional
    @Query(value = "select v from ValueDO v  where v.epoch = :epoch")
    List<ValueDO> findValuesForEpoch(int epoch );



    @Transactional
    @Query(value = "select v from ValueDO v  where v.timestep.id = :timestepId")
    List<ValueDO> findValuesForTimeStepId(long timestepId);

    @Transactional
    @Query(value = "select v.timestep.episode.id from ValueDO v  where v.epoch = :epoch")
    List<Long> findEpisodeIdsWithAValueEntry(int epoch);


    @Transactional
    @Query(value = "select v.timestep from ValueDO v where v.timestep.exploring = false and v.epoch = :epoch")
    List<TimeStepDO> findNonExploringTimeStepWithAValueEntry(int epoch);

    @Transactional
    @Modifying
    @Query(value = "insert into value (id, epoch, value, timestep_id, value_mean, value_hat_squared_mean, count) values (nextval('value_seq'), :epoch, :value, :timestep_id, 0, 0, 0);", nativeQuery = true )
    void myInsert(int epoch, double value, long timestep_id);



    public static Optional<ValueDO> extractValueDO(List<ValueDO>valueDOs, int epoch) {
        for(ValueDO valueDO : valueDOs) {
            if (valueDO.getEpoch() == epoch) return Optional.of(valueDO);
        }
        return Optional.empty();
    }
}
