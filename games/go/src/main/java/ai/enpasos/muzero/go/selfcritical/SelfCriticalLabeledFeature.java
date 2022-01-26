package ai.enpasos.muzero.go.selfcritical;

import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SelfCriticalLabeledFeature {

    static double maxEntropy = Math.log(2);


    // raw label data
     OneOfTwoPlayer winner;

    // raw input data
    float value;
    OneOfTwoPlayer toPlay;
 //   int numberOfMovesPlayedSofar;


    boolean correct;
    // boolean correctAndNoMindChange;

    // normalized input data
    double entropy;
 //   double toPlayNormalized;

    public void transformRawToPreNormalizedInput() {
        value = value * (toPlay == OneOfTwoPlayer.PLAYER_A ?  1f : -1f) ;

        value = (value + 1.0f) / 2.0f;

        entropy = - value * Math.log(value) - (1.0 - value) * Math.log(1.0 - value);
        entropy = entropy/ maxEntropy;

      //  toPlayNormalized = toPlay.getActionValue();

        correct  = (OneOfTwoPlayer.PLAYER_A == winner) ? value > 0.5 : value < 0.5;

    }



}
