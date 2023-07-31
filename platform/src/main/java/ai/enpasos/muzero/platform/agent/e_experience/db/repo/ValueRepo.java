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
    @Query(value = "select v from ValueDO v  where v.epoch = :epoch")
    List<ValueDO> findValuesForEpoch(int epoch );

    @Transactional
    @Query(value = "select v from ValueDO v  where v.timestep.id = :timestepId and v.archived = false")
    List<ValueDO> findNonArchivedValuesForTimeStepId(long timestepId);



    @Transactional
    @Modifying
    @Query(value = "insert into value (id, epoch, value, timestep_id, archived) values (nextval('value_seq'), :epoch, :value, :timestep_id,false);", nativeQuery = true )
    void myInsert(int epoch, double value, long timestep_id);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM value v WHERE epoch < :epoch", nativeQuery = true )
    void deleteValuesBeforeEpoch(int epoch);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM value v WHERE epoch =  :epoch", nativeQuery = true )
    void deleteValuesOfEpoch(int epoch);

    public static Optional<ValueDO> extractValueDOMaxEpoch(List<ValueDO>valueDOs) {
        int maxEpoch = -1;
        Optional<ValueDO> maxValueDO = Optional.empty();
        for(ValueDO valueDO : valueDOs) {
            if (valueDO.getEpoch() > maxEpoch) {
                maxEpoch = valueDO.getEpoch();
                maxValueDO = Optional.of(valueDO);
            }
        }
        return maxValueDO;
    }

    @Transactional
    @Modifying
    @Query(value = "update value v set archived = true from timestep t where v.archived = false and t.archived = true and v.timestep_id = t.id", nativeQuery = true )
    void markArchived(  );
}
