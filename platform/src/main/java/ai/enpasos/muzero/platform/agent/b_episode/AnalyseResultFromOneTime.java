package ai.enpasos.muzero.platform.agent.b_episode;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnalyseResultFromOneTime {
    Integer uOk;
    double normedSampleError;
}
