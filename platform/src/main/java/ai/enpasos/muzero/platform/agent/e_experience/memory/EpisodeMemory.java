package ai.enpasos.muzero.platform.agent.e_experience.memory;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;

public interface EpisodeMemory {



    void add(Game game);
    void remove(Game game);
    boolean visitsUnvisitedAction(Game game);

    boolean[] getLegalNotDeeplyVisitedActions(Game game, int t);


    void add(Game game, int t);
    void remove(Game game, int t);
    boolean visitsUnvisitedAction(Game game, int t);

    int getNumberOfStateNodes();

    int getNumberOfEpisodes();

    void setCapacity(int i);
}
