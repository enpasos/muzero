package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.LegalActionsDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LegalActionsRepo extends JpaRepository<LegalActionsDO,Long> {
//    boolean existsByLegalActions(boolean[] legalActions);

    @Transactional
    @Query(value = "select a from LegalActionsDO a  where a.legalActions in :legalActionsList")
    List<LegalActionsDO> findAllByLegalActions(List<boolean[]> legalActionsList);


    @Transactional
    @Query(value = "select e from LegalActionsDO e JOIN FETCH e.timeSteps t where e.id = :legalActionId")
    LegalActionsDO findLegalActionsDOWithTimeStepDOs(long legalActionId);

    @Transactional
    @Modifying
    @Query(value = "DROP TABLE IF EXISTS legalactions CASCADE", nativeQuery = true )
    void dropTable();

    @Transactional
    @Modifying
    @Query(value = "DROP SEQUENCE IF EXISTS legalactions_seq CASCADE", nativeQuery = true )
    void dropSequence();

}
