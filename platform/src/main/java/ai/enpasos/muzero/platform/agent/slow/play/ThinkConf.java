package ai.enpasos.muzero.platform.agent.slow.play;

import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThinkConf {

    ThinkBudget playerAConfig;
    ThinkBudget playerBConfig;

    public static ThinkConf instanceFromConfig(MuZeroConfig config) {
        return ThinkConf.builder()
                .playerAConfig(
                        ThinkBudget.builder()
                                .numSims(config.getNumSimulations())
                                .numParallel(config.getNumParallelPlays())
                                .numOfPlays(config.getNumPlays())
                                .build())
                .playerBConfig(
                        ThinkBudget.builder()
                                .numSims(config.getNumSimulations())
                                .numParallel(config.getNumParallelPlays())
                                .numOfPlays(config.getNumPlays())
                                .build())
                .build();
    }

    public int numOfPlays() {
        return playerAConfig.numParallel < playerBConfig.numParallel ? playerAConfig.getNumOfPlays() : playerBConfig.getNumOfPlays();
    }

    public int numParallelGames() {
        return Math.min(playerAConfig.numParallel, playerBConfig.numParallel);
    }

    public ThinkBudget thinkBudget(OneOfTwoPlayer player) {
        switch (player) {
            case PlayerA:
                return playerAConfig;
            case PlayerB:
                return playerBConfig;
        }
        return null;
    }
}
