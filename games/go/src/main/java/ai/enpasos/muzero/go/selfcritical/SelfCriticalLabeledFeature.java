package ai.enpasos.muzero.go.selfcritical;

import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SelfCriticalLabeledFeature {
    // raw label data
    boolean playerAWins;

    // raw input data
    float value;  // from rootValues already normalized to [0,1] and from the perspective of the player toPlay
    OneOfTwoPlayer toPlay;
    int numberOfMovesPlayedSofar;

    // normalized input data
    double entropy;  // not normalized
    double toPlayNormalized;
    double normalizedNumberOfMovesPlayedSofar;

    public void transformRawToPreNormalizedInput() {
        entropy = - value * Math.log(value) - (1.0 - value) * Math.log(1.0 - value);
        toPlayNormalized = toPlay.getActionValue();
    }



}
