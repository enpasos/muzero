package ai.enpasos.muzero.platform.agent.e_experience.memory;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;

import java.util.Collection;
import java.util.List;

public interface EpisodeMemory {

    void add(Game game);

    int getNumberOfEpisodes();

    void setCapacity(int i);

    int getAverageGameLength() ;

    int getMaxGameLength() ;

    List<Game> getGameList();

}
