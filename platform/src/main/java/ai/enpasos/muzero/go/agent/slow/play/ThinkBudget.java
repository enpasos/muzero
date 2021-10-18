package ai.enpasos.muzero.go.agent.slow.play;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThinkBudget {
    int numSims;
    int numParallel;
int numOfPlays;

}
