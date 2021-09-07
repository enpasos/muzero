package ai.enpasos.muzero.agent.slow.play;

import ai.enpasos.muzero.agent.slow.play.ThinkBudget;
import ai.enpasos.muzero.environments.OneOfTwoPlayer;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class ThinkConf {

    ThinkBudget  playerAConfig;
    ThinkBudget  playerBConfig;

    public int numOfPlays() {
        return playerAConfig.numParallel < playerBConfig.numParallel ? playerAConfig.getNumOfPlays() : playerBConfig.getNumOfPlays();
    }


    public int numParallelGames() {
        return Math.min(playerAConfig.numParallel, playerBConfig.numParallel);
    }

    public ThinkBudget thinkBudget(OneOfTwoPlayer player) {
        switch(player) {
            case PlayerA: return playerAConfig;
            case PlayerB: return playerBConfig;
        }
        return null;
    }
}
