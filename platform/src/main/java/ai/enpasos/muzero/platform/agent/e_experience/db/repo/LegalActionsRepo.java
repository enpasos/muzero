package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.LegalActionsDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface LegalActionsRepo extends JpaRepository<LegalActionsDO,Long> {
//    boolean existsByLegalActions(boolean[] legalActions);

    @Transactional
    @Query(value = "select a from LegalActionsDO a  where a.legalActions in :legalActionsList")
    List<LegalActionsDO> findAllByLegalActions(List<boolean[]> legalActionsList);

}
