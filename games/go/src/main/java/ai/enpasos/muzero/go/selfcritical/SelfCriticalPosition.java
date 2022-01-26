package ai.enpasos.muzero.go.selfcritical;

import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SelfCriticalPosition {
    int fullMove;
    OneOfTwoPlayer player;
}
