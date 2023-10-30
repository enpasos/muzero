package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.LegalActionsDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.StateNodeDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StateNodeRepo extends JpaRepository<StateNodeDO,Long> {
    @Transactional
    @Query(value = "select e from StateNodeDO e JOIN FETCH e.timeSteps t where e.id = :stateNodeId")
    StateNodeDO findStateNodeDOWithTimeStepDOs(long stateNodeId);

    @Transactional
    @Modifying
    @Query(value = "update StateNodeDO t set t.visitedActions = NULL ")
    void deleteVisitedActions();


    @Transactional
    @Modifying
    @Query(value = "update StateNodeDO t set t.visitedActions = :visited where t.id = :stateNodeDOId ")
    void updateVisitedActions(long stateNodeDOId, boolean[] visited);
}
