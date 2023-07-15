package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.ValueStatsDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ValueStatsRepo extends JpaRepository<ValueStatsDO,Long> {




}
