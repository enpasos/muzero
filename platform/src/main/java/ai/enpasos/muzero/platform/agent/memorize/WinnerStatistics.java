package ai.enpasos.muzero.platform.agent.memorize;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WinnerStatistics {
    long allGames;
    long winPlayerACount;
    long drawCount;
    long winPlayerBCount;
}
