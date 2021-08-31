package ai.enpasos.muzero.gamebuffer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WinnerStatistics {
    long winPlayerACount;
    long drawCount;
    long winPlayerBCount;
}
