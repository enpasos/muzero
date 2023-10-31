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

    private Set<StateNode> stateNodeSet = new HashSet<>();

    private Map<StateNode, Set<TimeStepDO>> mapStateNodeActions;


    public EpisodeMemoryImpl(int capacity) {
        this.capacity = capacity;
        mapStateNodeActions = new HashMap<>();
        gameList = new ArrayList<>();
    }

    @Override
    public void add(Game game) {
        game.getEpisodeDO().setGame(game);
        while (gameList.size() >= capacity) {
           Game gameToRemove = gameList.get(0);
           remove(gameToRemove);
        }
        gameList.add(game);
        for(TimeStepDO ts : game.getEpisodeDO().getTimeSteps()) {
            add(game, ts.getT());
        }
    }

    @Override
    public void remove(Game game) {
        gameList.remove(game);
    }

    @Override
    public boolean visitsUnvisitedAction(Game game) {
       for (int t = 0; t < game.getEpisodeDO().getLastTime(); t++) {
           if (visitsUnvisitedAction(game, t)) {
               return true;
           }
       }
        return false;
    }

    @Override
    public boolean visitsUnvisitedAction(Game game, int t) {
        StateNode stateNode = new ObservationStateNode(game, t);
        List<StateNode> visitedStateNodes = getVisitedStateNodes(stateNode);
        StateNode nextStateNode = new ObservationStateNode(game, t+1);
        return !visitedStateNodes.contains(nextStateNode);
    }


    private List<StateNode> getVisitedStateNodes(StateNode stateNode) {
        Set<TimeStepDO> timeSteps = this.mapStateNodeActions.get(stateNode);
        if(timeSteps == null) return new ArrayList<>();
        return timeSteps.stream().map( ts -> new ObservationStateNode(ts.getEpisode().getGame(), ts.getT()+1)).collect(Collectors.toList());
    }

    private boolean areAllStateNodesVisited(TimeStepDO timeStepDO) {
        StateNode stateNode = new ObservationStateNode(timeStepDO.getEpisode().getGame(), timeStepDO.getT());
        Set<TimeStepDO> timeSteps = this.mapStateNodeActions.get(stateNode);
        boolean[] legalActions = timeStepDO.getLegalact().getLegalActions();
        int numberOfLegalActions = 0;
        for (int i = 0; i < legalActions.length; i++) {
            if (legalActions[i]) numberOfLegalActions++;
        }
        return timeSteps.size() == numberOfLegalActions;
    }

    @Override
    public boolean[] getLegalNotDeeplyVisitedActions(Game game, int t) {
        boolean[] legalActionsPlus =  getLegalNotVisitedActions(game, t);
        StateNode stateNode = new ObservationStateNode(game, t);
        Set<TimeStepDO> timeSteps = this.mapStateNodeActions.get(stateNode);
        if(timeSteps != null) {
            for (TimeStepDO timeStepDO : timeSteps) {
                if (timeStepDO != null && timeStepDO.getAction() != null) {
                    legalActionsPlus[timeStepDO.getAction()] = isStateNodeVisitedDeeply(timeStepDO.getNextTimeStep());
                }
            }
        }
        return legalActionsPlus;
    }


    private boolean[] getLegalNotVisitedActions(Game game, int t) {
        boolean[] legalActionsPlus = game.getEpisodeDO().getTimeStep(t).getLegalact().getLegalActions();
        StateNode stateNode = new ObservationStateNode(game, t);
        Set<TimeStepDO> timeSteps = this.mapStateNodeActions.get(stateNode);
        if(timeSteps != null) {
            for (TimeStepDO timeStepDO : timeSteps) {
                if (timeStepDO != null && timeStepDO.getAction() != null) {
                    legalActionsPlus[timeStepDO.getAction()] = false;
                }
            }
        }
        return legalActionsPlus;
    }

    private boolean isStateNodeVisitedDeeply(TimeStepDO timeStepDO) {
        StateNode stateNode = new ObservationStateNode(timeStepDO.getEpisode().getGame(), timeStepDO.getT());
        Set<TimeStepDO> timeSteps = this.mapStateNodeActions.get(stateNode);

        boolean[] legalActions = timeStepDO.getLegalact().getLegalActions();
        int numberOfLegalActions = 0;
        for (int i = 0; i < legalActions.length; i++) {
            if (legalActions[i]) numberOfLegalActions++;
        }
        if(timeSteps == null) return numberOfLegalActions == 0;
        if (timeSteps.size() != numberOfLegalActions) return false;
        for(TimeStepDO ts : timeSteps) {
            TimeStepDO nextTimeStep = ts.getNextTimeStep();
            if (nextTimeStep!= null && !isStateNodeVisitedDeeply(nextTimeStep)) return false;
        }
        return true;
    }

    /**
     * Add a time step to the state memory. If an instance of the same time step already exists, it is removed first. This
     * assures that the most recent version of the time step is stored.
     */
    @Override
    public void add(Game game, int t) {
        //System.out.println("add t=" + t);
        StateNode stateNode = new ObservationStateNode(game, t);   // TODO configurable factory
        if (t < game.getEpisodeDO().getLastTime() ) {
            Set<TimeStepDO> episodeNodes = mapStateNodeActions.get(stateNode);
            if (episodeNodes == null) {
                episodeNodes = new HashSet<>();
                mapStateNodeActions.put(stateNode, episodeNodes);
            }
            TimeStepDO timeStepDO = game.getEpisodeDO().getTimeStep(t);
            episodeNodes.remove(timeStepDO);
            episodeNodes.add(timeStepDO);
        }
        stateNodeSet.remove(stateNode);
        stateNodeSet.add(stateNode);
    }

    @Override
    public void remove(Game game, int t) {
        StateNode stateNode = new ObservationStateNode(game, t);
        stateNodeSet.remove(stateNode);
    }



    @Override
    public int getNumberOfStateNodes() {
        return this.stateNodeSet.size();
    }

    @Override
    public int getNumberOfEpisodes() {
        return this.gameList.size();
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int getAverageGameLength() {
        return (int)this.gameList.stream().mapToInt(g -> g.getEpisodeDO().getLastTime()+1).average().orElse(1000);
    }

    @Override
    public int getMaxGameLength() {
        return this.gameList.stream().mapToInt(g -> g.getEpisodeDO().getLastTime()+1).max().orElse(0);
    }

    @Override
    public List<Game> getGameList() {
        return new ArrayList<>(gameList);
    }


}
