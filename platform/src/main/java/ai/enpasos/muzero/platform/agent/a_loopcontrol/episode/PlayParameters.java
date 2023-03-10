package ai.enpasos.muzero.platform.agent.a_loopcontrol.episode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayParameters {
    @Builder.Default
    boolean render = true;
    @Builder.Default
    boolean fastRulesLearning = false;
    @Builder.Default
    boolean drawNotMaxWhenJustWithInitialInference = false;

    @Builder.Default
    boolean untilEnd = true;
    @Builder.Default
    boolean justInitialInferencePolicy = false;

    @Builder.Default
    boolean justReplayWithInitialReference = false;
    @Builder.Default
    boolean withRandomActions = true;

    int averageGameLength;

    double pRandomActionRawAverage;


    boolean replay;

}
