package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.LegalActionsDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.StateNodeDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StateNodeRepo extends JpaRepository<StateNodeDO,Long> {

}
