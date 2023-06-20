package ai.enpasos.muzero.platform.agent.e_experience.repo;

import ai.enpasos.muzero.platform.agent.e_experience.domain.EpisodeDO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepo extends JpaRepository<EpisodeDO,Long> {

}
