package ai.enpasos.muzero.platform.agent.e_experience.memory;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;

import java.sql.Time;
import java.util.*;
import java.util.stream.Collectors;

public class EpisodeMemoryImpl implements EpisodeMemory {


    private int capacity;
    private List<Game> gameList;



    public EpisodeMemoryImpl(int capacity) {
        this.capacity = capacity;
        gameList = new ArrayList<>();
    }

    @Override
    synchronized public void add(Game game) {
        game.getEpisodeDO().setGame(game);
        while (gameList.size() >= capacity) {
           gameList.remove(0);
        }
        gameList.add(game);
    }





    @Override
    synchronized public int getNumberOfEpisodes() {
        return this.gameList.size();
    }

    @Override
    synchronized public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    synchronized public int getAverageGameLength() {
        return (int)this.gameList.stream().mapToInt(g -> g.getEpisodeDO().getLastTime()+1).average().orElse(1000);
    }

    @Override
    synchronized public int getMaxGameLength() {
        return this.gameList.stream().mapToInt(g -> g.getEpisodeDO().getLastTime()+1).max().orElse(0);
    }

    @Override
    synchronized public List<Game> getGameList() {
        return new ArrayList<>(gameList);
    }


}
